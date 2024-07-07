package org.zeuscommerce.app.Repo;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import org.zeuscommerce.app.Entity.Product;
import org.zeuscommerce.app.Util.ProductStatus;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
public interface  ProductsRepo extends ReactiveMongoRepository<Product,String> {
    Flux<Product> findAllByIdInAndStatus(List<String> id, ProductStatus status);
}
