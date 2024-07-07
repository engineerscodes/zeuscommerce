package org.zeuscommerce.app.Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zeuscommerce.app.Dto.OrderDto;
import org.zeuscommerce.app.Entity.Order;
import org.zeuscommerce.app.Service.OrderService;
import org.zeuscommerce.app.Util.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/order")
public class OrderController {


    @Autowired
    OrderService orderService;


    @GetMapping
    public Flux<Order> getAllOrders(){
        return orderService.getAllOrders();
    }


    @GetMapping("/{orderId}")
    public Object getOrderById(@PathVariable String orderId){
        return orderService.getOrderById(orderId).map(ResponseEntity::ok).cast(ResponseEntity.class)
                .onErrorResume(e->Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @PutMapping("/{orderId}")
    public Object updateOrder(@PathVariable String orderId, @RequestBody OrderDto orderDto){
        return orderService.updateOrder(orderId,orderDto).map(ResponseEntity::ok).cast(ResponseEntity.class)
                .onErrorResume(e->Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @PutMapping("/{orderId}/status")
    public Object updateOrderStatus(@PathVariable String orderId, @RequestBody OrderStatus status){
        return orderService.updateOrderStatus(orderId,status).map(ResponseEntity::ok).cast(ResponseEntity.class)
                .onErrorResume(e->Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @PostMapping("/newOrder")
    public Object insertNewOrder(@RequestBody OrderDto orderDto){
        return orderService.insertNewOrder(orderDto).map(ResponseEntity::ok).cast(ResponseEntity.class)
                .onErrorResume(e->Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }


    @PutMapping("/{orderId}/rushFlag")
    public Object updateRushFlag(@PathVariable String orderId,@RequestBody boolean rushFlag){
        return orderService.updateRushFlag(orderId,rushFlag).map(ResponseEntity::ok).cast(ResponseEntity.class)
                .onErrorResume(e->Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }



}
