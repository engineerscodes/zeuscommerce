package org.zeuscommerce.app.Service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.zeuscommerce.app.Entity.Order;
import org.zeuscommerce.app.Repo.OrderRepo;
import org.zeuscommerce.app.Repo.ProductsRepo;
import org.zeuscommerce.app.Util.DeliveryOrderStatus;
import org.zeuscommerce.app.Util.OrderStatus;
import org.zeuscommerce.app.Util.PlacedProduct;
import org.zeuscommerce.app.Util.ProductStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeliveryService {

    @Autowired
    OrderRepo orderRepo;

    @Autowired
    ProductsRepo productsRepo;

    @Autowired
    OrderService orderService;

    @Autowired
    ProductService productService;

    public Flux<Order> getAllDelivery() {
        return orderRepo.findAllByStatusIn(List.of(OrderStatus.Confirmed,OrderStatus.OutForDelivery,OrderStatus.Delivered,OrderStatus.Dispute,OrderStatus.Cancel));
    }

    public Flux<Order> getAllDeliveryByStatus(DeliveryOrderStatus orderStatus) {
        return orderRepo.findAllByStatus(orderStatus);
    }

    public Mono<OrderStatus> getDeliveryStatus(String orderId) {
        return orderRepo.findById(orderId).map(Order::getStatus).
                switchIfEmpty(Mono.error(new RuntimeException("Product Not found")));
    }



    public Mono<Order> updateDeliveryStatus(String orderId,DeliveryOrderStatus orderStatus) {
        return orderService.findOrder(orderId).flatMap(order->{
                if(orderStatus.equals(DeliveryOrderStatus.Cancel) && order.getStatus().equals(OrderStatus.Delivered)){
                    return Mono.error(new RuntimeException("Can't cancel for order that's Delivered"));
                }else if(orderStatus.equals(DeliveryOrderStatus.Cancel) && order.getStatus().ordinal()< OrderStatus.Delivered.ordinal()){

                    productService.findAllByID(order.getPlacedProducts().stream().map(PlacedProduct::getProductId).toList()).flatMap(product -> {
                        product.setStatus(ProductStatus.UNSOLD);
                        productsRepo.save(product).subscribe();
                        order.setStatus(OrderStatus.valueOf(orderStatus.name()));
                        return orderRepo.save(order);
                    });
                }else if(orderStatus.equals(DeliveryOrderStatus.Dispute) && !order.getStatus().equals(OrderStatus.Delivered)){
                    return Mono.error(new RuntimeException("You can only Dispute for Order that's Delivered"));
                }else if (order.getStatus().equals(OrderStatus.Cancel)){
                    return Mono.error(new RuntimeException("Cancelled Order can't be updated"));
                }
                System.out.println(OrderStatus.valueOf(orderStatus.name()));
                order.setStatus(OrderStatus.valueOf(orderStatus.name()));
                return orderRepo.save(order);
        });
    }

    public Mono<Order> updateDeliveryDateTime(String orderId, LocalDateTime newDateTime) {
        return orderService.findOrder(orderId).flatMap(order->{
            Assert.isTrue(order.getStatus().ordinal()>OrderStatus.InProgress2Confirm.ordinal(),"Can't update for Order status Less than confirmed");
            Map<String,Object> orderDetails = new HashMap<>(order.getOrderDetails() != null ? order.getOrderDetails() : Map.of());
            orderDetails.put("DeliveryDateTime",newDateTime);
            order.setOrderDetails(orderDetails);
            return orderRepo.save(order);
        });
    }
}
