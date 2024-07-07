package org.zeuscommerce.app;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.zeuscommerce.app.Entity.Order;
import org.zeuscommerce.app.Entity.Product;
import org.zeuscommerce.app.Repo.OrderRepo;
import org.zeuscommerce.app.Repo.ProductsRepo;
import org.zeuscommerce.app.Util.Address;
import org.zeuscommerce.app.Util.OrderStatus;
import org.zeuscommerce.app.Util.PlacedProduct;
import org.zeuscommerce.app.Util.ProductStatus;
import reactor.core.publisher.Flux;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


@EnableCaching
@SpringBootApplication
public class Application {

    public static void main(String[] agr){
        SpringApplication.run(Application.class,agr);
    }



    @Bean
    public ApplicationRunner initializer(ProductsRepo repository, OrderRepo orderRepo) {
        return args -> {
            Product p1 = Product.builder().name("Product 1").
                    description("Description for product 1")
                    .price(20.0).metaData(Map.of("color", "red"))
                    .cost(1)
                    .status(ProductStatus.UNSOLD)
                    .quantity(2)
                    .latitude(25.927230).longitude(-80.228480).build();

            Product p2 = Product.builder().name("Product 2").
                    description("Description for product 2")
                    .price(300.0).metaData(Map.of("color", "blue"))
                    .cost(3)
                    .status(ProductStatus.UNSOLD)
                    .quantity(5)
                    .latitude(25.927230).longitude(-80.228480).build();

            Product p3 = Product.builder().name("Product 2").
                    description("Description for product 3")
                    .price(750.0).metaData(Map.of("color", "yellow"))
                    .cost(5)
                    .status(ProductStatus.UNSOLD)
                    .quantity(15)
                    .latitude(25.927230).longitude(-80.228480).build();


            List<Product> products = repository.deleteAll()
                    .thenMany(Flux.just(p1, p2,p3)
                            .flatMap(repository::save)).collectList().block(); //blocks

            Assert.isTrue(products!=null,"Can't be Null");
            Order o1 = Order.builder().orderDetails(Map.of("address",Address.builder().latitude(20.927230).longitude(-70.228480).build()))
                    .status(OrderStatus.Draft).orderProductSum(BigDecimal.valueOf(products.get(1).getPrice()+products.get(0).getPrice()))
                    .placedProducts(Set.of(PlacedProduct.builder().productId(products.get(1).getId()).quantity(products.get(1).getQuantity()).build()
                            ,PlacedProduct.builder().productId(products.get(0).getId()).quantity(products.get(0).getQuantity()).build()))
                    .build();

            Order o2 = Order.builder().orderDetails(Map.of("address",Address.builder().latitude(20.927230).longitude(-70.228480).build()))
                    .status(OrderStatus.Draft).orderProductSum(BigDecimal.valueOf(products.get(2).getPrice()+products.get(1).getPrice()))
                    .placedProducts(Set.of(PlacedProduct.builder().productId(products.get(1).getId()).quantity(products.get(1).getQuantity()).build()
                            ,PlacedProduct.builder().productId(products.get(2).getId()).quantity(products.get(2).getQuantity()).build()))
                    .rushOrder(true)
                    .build();

            Order o3 = Order.builder().orderDetails(Map.of("address",Address.builder().latitude(0).longitude(-70.228480).build()))
                    .status(OrderStatus.Draft).orderProductSum(BigDecimal.valueOf(products.get(2).getPrice()+products.get(1).getPrice()))
                    .rushOrder(true)
                    .placedProducts(Set.of(PlacedProduct.builder().productId(products.get(1).getId()).quantity(products.get(1).getQuantity()).build()
                            ,PlacedProduct.builder().productId(products.get(2).getId()).quantity(products.get(2).getQuantity()).build()))
                    .build();

            Order o4 = Order.builder().orderDetails(Map.of("address",Address.builder().latitude(0).longitude(-70.228480).build()))
                    .status(OrderStatus.Confirmed).orderProductSum(BigDecimal.valueOf(products.get(2).getPrice()+products.get(1).getPrice()))
                    .placedProducts(Set.of(PlacedProduct.builder().productId(products.get(1).getId()).quantity(products.get(1).getQuantity()).build()
                            ,PlacedProduct.builder().productId(products.get(2).getId()).quantity(products.get(2).getQuantity()).build()))
                    .rushOrder(true)
                    .build();


            orderRepo.deleteAll().thenMany(Flux.just(o1,o2,o3,o4))
                    .flatMap(orderRepo::save)
                    .thenMany(orderRepo.findAll())
                    .subscribe(order -> System.out.println("Inserted orders: " + order));

        };
    }

}
