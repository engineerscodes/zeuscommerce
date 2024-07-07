package org.zeuscommerce.app.Messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.SerializationUtils;
import org.zeuscommerce.app.Entity.Order;
import org.zeuscommerce.app.Entity.Product;
import org.zeuscommerce.app.Repo.ProductsRepo;
import org.zeuscommerce.app.Util.OrderMsg;
import org.zeuscommerce.app.Util.PlacedProduct;
import org.zeuscommerce.app.Util.ProductStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.rabbitmq.*;

import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Component
public class RabbitClient {


    public static final String EXCHANGE_NAME = "zeus-exchange";
    public static final String QUEUE_NAME_1 = "zeus-queue-1";
    public static final String QUEUE_NAME_2 = "zeus-queue-2";
    public static final String QUEUE_NAME_3 = "zeus-queue-3";
    public static final String ROUTING_KEY_1 = "zeus.orders.lot.*";
    public static final String ROUTING_KEY_2 = "zeus.orders.few.*";

    public static final String ROUTING_KEY_3 = "zeus.order.#";

    @Autowired
    Sender RabbitSender;

    @Autowired
    ProductsRepo productsRepo;


    @PostConstruct
    public void setup() {
        RabbitSender.declareExchange(ExchangeSpecification.exchange(EXCHANGE_NAME).type("topic").durable(true))
                .then(RabbitSender.declareQueue(QueueSpecification.queue(QUEUE_NAME_1).durable(true)))
                .then(RabbitSender.declareQueue(QueueSpecification.queue(QUEUE_NAME_2).durable(true)))
                .then(RabbitSender.declareQueue(QueueSpecification.queue(QUEUE_NAME_3).durable(true)))
                .then(RabbitSender.bind(BindingSpecification.binding(EXCHANGE_NAME, ROUTING_KEY_1, QUEUE_NAME_1)))
                .then(RabbitSender.bind(BindingSpecification.binding(EXCHANGE_NAME, ROUTING_KEY_2, QUEUE_NAME_2)))
                .then(RabbitSender.bind(BindingSpecification.binding(EXCHANGE_NAME, ROUTING_KEY_3, QUEUE_NAME_3)))
                .subscribe();
    }

    public void sendMessage(Order savedOrder){
        log.info("Sending message to exchange");
        ObjectMapper mapper = new ObjectMapper();
            productsRepo.findAllById(savedOrder.getPlacedProducts().stream().map(PlacedProduct::getProductId).toList()).collectList().flatMapMany(products -> {
                /*Map<String, Long> idVersionQuantityMap = products.stream()
                        .collect(Collectors.toMap(Product::getId, Product::getVersion));

                 */
                Map<String, Map<String, Long>> idVersionQuantityMap = products.stream()
                        .collect(Collectors.toMap(
                                Product::getId,
                                product -> Map.of("productVersion",product.getVersion(),"quantity",product.getQuantity().longValue())
                                )
                        );
                OrderMsg msg = OrderMsg.builder().id(savedOrder.getId()).OrderVersion(savedOrder.getVersion()).productVersion(idVersionQuantityMap).build();
                try {
                    byte[] jsonBytes = mapper.writeValueAsBytes(msg);
                    Flux<OutboundMessage> outboundMessageFlux =
                            Flux.just(new OutboundMessage(EXCHANGE_NAME,getRoutingKey(products.size()),jsonBytes));
                    RabbitSender.sendWithPublishConfirms(outboundMessageFlux).doOnError(e->log.error("Error sending data to queue")).subscribe();
                } catch (JsonProcessingException e) {
                    return Mono.error(new RuntimeException("Error sending message to exchange"));
                }
                return Mono.create(MonoSink::success);
            }).subscribe();
    }

    public String getRoutingKey(int noOfProducts){
        if(noOfProducts>=10) return "zeus.orders.lot.product";
        else if(noOfProducts>=3) return "zeus.orders.few.product";
        else return "zeus.order.small";
    }

}
