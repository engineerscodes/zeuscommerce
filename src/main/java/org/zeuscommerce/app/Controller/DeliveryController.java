package org.zeuscommerce.app.Controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zeuscommerce.app.Entity.Order;
import org.zeuscommerce.app.Service.DeliveryService;
import org.zeuscommerce.app.Util.DeliveryOrderStatus;
import org.zeuscommerce.app.Util.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;


@RestController
@RequestMapping("/delivery")
public class DeliveryController {

    @Autowired
    DeliveryService deliveryService;

    @GetMapping
    public Flux<Order> getAllDelivery(@RequestParam(required = false) DeliveryOrderStatus orderStatus){
        return orderStatus==null?deliveryService.getAllDelivery():deliveryService.getAllDeliveryByStatus(orderStatus);
    }

    @GetMapping("/{orderId}/status")
    public Mono<ResponseEntity<String>> getDeliveryStatus(@PathVariable String orderId){
        return deliveryService.getDeliveryStatus(orderId)
                .map(status-> ResponseEntity.ok(String.format("status of order is : %s",status)))
                .onErrorResume(e-> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @PutMapping("/{orderId}/status")
    public Object updateDeliveryStatus(@PathVariable String orderId,@RequestBody DeliveryOrderStatus orderStatus){
        return deliveryService.updateDeliveryStatus(orderId,orderStatus)
                .map(ResponseEntity::ok)
                .cast(ResponseEntity.class)
                .onErrorResume(e-> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @PutMapping("/{orderId}/changeInTime")
    public Object updateDeliveryStatus(@PathVariable String orderId, @RequestBody LocalDateTime newDateTime){
        return deliveryService.updateDeliveryDateTime(orderId,newDateTime)
                .map(ResponseEntity::ok)
                .cast(ResponseEntity.class)
                .onErrorResume(e-> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }


}
