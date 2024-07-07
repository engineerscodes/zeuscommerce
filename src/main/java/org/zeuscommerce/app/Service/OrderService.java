package org.zeuscommerce.app.Service;



import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.zeuscommerce.app.Dto.OrderDto;
import org.zeuscommerce.app.Entity.Order;
import org.zeuscommerce.app.Entity.Product;
import org.zeuscommerce.app.Mapper.OrderMapper;
import org.zeuscommerce.app.Messaging.RabbitClient;
import org.zeuscommerce.app.Repo.OrderRepo;
import org.zeuscommerce.app.Repo.ProductsRepo;
import org.zeuscommerce.app.Util.Address;
import org.zeuscommerce.app.Util.OrderStatus;
import org.zeuscommerce.app.Util.PlacedProduct;
import org.zeuscommerce.app.Util.ProductStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {


    BigDecimal Limit = new BigDecimal(100);

    @Autowired
    OrderRepo orderRepo;

    @Autowired
    ProductsRepo productsRepo;

    @Autowired
    ProductService productService;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    RabbitClient rabbitClient;



    public Flux<Order> getAllOrders() {
        return orderRepo.findAll();
    }

    public Mono<Order> findOrder(String orderId){
        return orderRepo.findById(orderId).
                switchIfEmpty(Mono.error(new RuntimeException("No Order found")));
    }

    public Mono<Order> getOrderById(String orderId) {
        return findOrder(orderId);
    }


    public Mono<Order> updateRushFlag(String orderId,boolean rushFlag) {
        Mono<Order> orderMono = findOrder(orderId);
        return orderMono.flatMap(order ->updateRushFlagAndValidatePrice(order, rushFlag));

    }

 //HERE
    private Mono<Order> updateRushFlagAndValidatePrice(Order order, boolean rushFlag) {
        Assert.isTrue(order.getStatus().equals(OrderStatus.Draft),"Can only Update Order in draft");
        return productService.findAllByID(order.getPlacedProducts().stream().map(PlacedProduct::getProductId).toList())
                .map(product -> product.getPrice()*product.getQuantity())
                .reduce(BigDecimal.ZERO,(total, fee) -> total.add(BigDecimal.valueOf(fee)))
                .flatMap(sum->{
                    if (rushFlag && sum.compareTo(Limit)<0)
                        return Mono.error(new RuntimeException("Sum of Product is less than 100"));
                    else {
                        order.setRushOrder(rushFlag);
                        return orderRepo.save(order);
                    }
                });
    }


    //here
    private Mono<Order> updateOrderAndValidatePrice(Order order,OrderDto orderDto) {
        Assert.isTrue(order.getStatus().equals(OrderStatus.Draft),"Can only Update Order in draft");
        return productService.findAllByID(orderDto.getPlacedProducts().stream().map(PlacedProduct::getProductId).toList())
                .collectList()
                .flatMapMany(db_r->{
                    Set<PlacedProduct> placedProduct = orderDto.getPlacedProducts();
                    BigDecimal total = new BigDecimal(0);
                    Map<String,Double> priceMap= db_r.stream().collect(Collectors.toMap(Product::getId,Product::getPrice));
                    Map<String,Integer> quantityMap =db_r.stream().collect(Collectors.toMap(Product::getId,Product::getQuantity));
                    for(PlacedProduct p:placedProduct){
                        if(p.getQuantity()<=quantityMap.get(p.getProductId())) {
                            total = total.add(BigDecimal.valueOf(p.getQuantity() * priceMap.get(p.getProductId())));
                            /*productService.findProduct(p.getProductId()).flatMap(_p->{
                                _p.setQuantity(_p.getQuantity()-p.getQuantity());
                                productsRepo.save(_p).subscribe();
                                return Mono.empty();
                            }).subscribe();
                             */ // Update when its confirmed order not in draft
                        }else {
                            return Mono.error(new RuntimeException("Product Quantity in input is more than available Quantity"));
                        }
                    }
                    //productsRepo.save() //save new qua
                    System.out.println(total);
                    if (orderDto.isRushOrder() && total.compareTo(Limit)<0)
                        return Mono.error(new RuntimeException("Sum of Product is less than 100"));
                    else {
                        order.setOrderProductSum(total);
                        orderMapper.OrderDtoToOrder(orderDto,order);
                        if (order.getStatus().equals(OrderStatus.Placed) && order.isRushOrder()) {
                            return updateOrderStatus(order.getId(),OrderStatus.Placed);
                        }
                        return orderRepo.save(order);
                    }
                }).single();
    }

    public Mono<Order> updateOrder(String orderId, OrderDto orderDto) {
        Mono<Order> orderMono = findOrder(orderId);
        return orderMono.flatMap(order ->updateOrderAndValidatePrice(order,orderDto));
    }

    public Mono<Order> updateOrderStatus(String orderId, OrderStatus status) {
        Mono<Order> orderMono = findOrder(orderId);
        return orderMono.flatMap(order ->{
            if (order.getStatus().ordinal()<=status.ordinal() && !status.equals(OrderStatus.Cancel) ){
                order.setStatus(status);
                return isFeasible(order).flatMap(feasible->{
                    log.info("feasible : {}",feasible);
                    if (feasible) {
                        return orderRepo.save(order).doOnSuccess(savedOrder -> {
                            if (status.equals(OrderStatus.Placed) && savedOrder.isRushOrder()) {
                                rabbitClient.sendMessage(order);
                            } else
                                log.info("ONLY RUSH FEATURE Implemented");
                        });
                    }else {
                        return Mono.error(new RuntimeException("Not feasible too far order,remove rush flag or change address of order"));
                    }
                });
            }else {
                if(status.equals(OrderStatus.Cancel) && order.getStatus().ordinal()<OrderStatus.Confirmed.ordinal()){
                    order.setStatus(status);
                    Set<String > products = order.getPlacedProducts().stream().map(PlacedProduct::getProductId).collect(Collectors.toSet());
                    Mono<List<Product>> productMono = productService.findAllByID(new ArrayList<>(products)).collectList();
                    return Mono.zip(orderRepo.save(order),productMono).flatMap(tuple->{
                        Order _order = tuple.getT1();
                        List<Product> _products = tuple.getT2();
                        Map<String,Integer> map=_order.getPlacedProducts().stream()
                                .collect(Collectors.toMap(PlacedProduct::getProductId,PlacedProduct::getQuantity));
                        _products.forEach(product->{
                            Long orderedQuantity = map.get(product.getId()).longValue();
                            Long availableQuantity = product.getQuantity().longValue();
                            log.info("Order data : AvailableQuantity {} / orderedQuantity {}",
                                    availableQuantity,orderedQuantity);
                            Assert.isTrue(orderedQuantity+availableQuantity>=0,"Can't be Negative");
                            product.setQuantity((int)(orderedQuantity+availableQuantity));
                            if(product.getQuantity()>0) product.setStatus(ProductStatus.UNSOLD);
                            productsRepo.save(product).subscribe();
                            });
                        return Mono.just(_order);
                    });
                }
                return Mono.error(new RuntimeException("Can't revert the status and can only cancel before confirmed"));
            }
        }).doOnError(e->log.info("Error {},",e.getMessage()));
    }

    public Mono<Order> insertNewOrder(OrderDto orderDto) {
        return productService.findAllByID(orderDto.getPlacedProducts().stream().map(PlacedProduct::getProductId).toList())
                .map(product -> product.getPrice()*product.getQuantity())
                .reduce(BigDecimal.ZERO,(total, fee) -> total.add(BigDecimal.valueOf(fee)))
                .flatMap(sum->{
                    System.out.println(sum);
                    if (orderDto.isRushOrder() && sum.compareTo(Limit)<0)
                        return Mono.error(new RuntimeException("Sum of Product is less than 100"));
                    else {
                        Order order = Order.builder().build();
                        order.setOrderProductSum(sum);
                        orderMapper.OrderDtoToOrder(orderDto,order);
                        return orderRepo.save(order);
                    }
                });
    }

    public Mono<Boolean> isFeasible(Order order){
        Map<String, Object> orderDetails=  new HashMap<>(order.getOrderDetails() != null ? order.getOrderDetails() : Map.of());
        Assert.isTrue(orderDetails.get("address")!=null,"Please Update order with Latitude  and Longitude inside Order Details ->address ");
        Address address = (Address) order.getOrderDetails().get("address");
        log.info("Lat {} and long {}",address.getLatitude(),address.getLongitude());
        return productService.findAllByIdAndProductStatus(order.getPlacedProducts().stream().map(PlacedProduct::getProductId).toList())
                .collectList().flatMapMany(products -> {
                    boolean possible = true;
                    for(Product product : products) {
                        System.out.println(product.getLatitude()+" --- "+(address.getLatitude() + 10));
                        if (product.getLatitude() < address.getLatitude() + 10) {
                            possible = false;
                            break;
                        }
                    }
                    return Flux.just(possible);
                }).single();
    }
}
