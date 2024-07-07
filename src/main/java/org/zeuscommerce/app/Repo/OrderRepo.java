package org.zeuscommerce.app.Repo;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import org.zeuscommerce.app.Entity.Order;
import org.zeuscommerce.app.Util.DeliveryOrderStatus;
import org.zeuscommerce.app.Util.OrderStatus;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface OrderRepo extends ReactiveMongoRepository<Order,String> {

    Flux<Order> findAllByStatusIn(List<OrderStatus> status);

    Flux<Order> findAllByStatus(DeliveryOrderStatus status);

}
