package org.zeuscommerce.app.Service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.zeuscommerce.app.Dto.ProductDto;
import org.zeuscommerce.app.Entity.Product;
import org.zeuscommerce.app.Mapper.ProductMapper;
import org.zeuscommerce.app.Repo.ProductsRepo;
import org.zeuscommerce.app.Util.ProductStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductService {

    @Autowired
    ProductsRepo productsRepo;


    @Autowired
    ProductMapper productMapper;


    public Flux<Product> getProducts() {
        System.out.println("DB CALL");
        return productsRepo.findAll();
    }

    public Mono<ProductDto> saveNewProduct(ProductDto p){
        Assert.isTrue(p.getStatus().equals(ProductStatus.UNSOLD),"Status of a New Product can only be UnSold");
        Assert.isTrue(p.getQuantity()>0,"Product Quantity must be greater than zero");
        return productsRepo.save(productMapper.ProductDtoToProduct(p))
                .map(productMapper::ProductToProductDto)
        .doOnSuccess(savedProduct ->log.info("New Product added : {}",savedProduct))
                        .doOnError(e-> log.error("Error : {}",e.getMessage()));
    }

    public Mono<Product> findProduct(String productId){
        return productsRepo.findById(productId).
                switchIfEmpty(Mono.error(new RuntimeException("Product Not found")));
    }

    public Mono<Product> updateProduct(String productId,ProductDto updateProduct){
        Mono<Product> productMono = findProduct(productId);
        return productMono.flatMap(product ->{
            Assert.isTrue(product.getStatus()!=null && !product.getStatus().equals(ProductStatus.SOLD),"Can't update status of sold Product");
            productMapper.UpdateProduct(updateProduct,product);
            return productsRepo.save(product);
        }).doOnSuccess(savedProduct ->log.info("Update Product with Id : {}",savedProduct.getId()))
        .doOnError(e-> log.error("Error : {}",e.getMessage()));

    }

    public Mono<Product> deleteProduct(String productId) {
        Mono<Product> productMono = findProduct(productId);
        return productMono.flatMap(product ->{
            Assert.isTrue(product.getStatus()!=null && product.getStatus().equals(ProductStatus.UNSOLD)
                    &&product.getQuantity()!=0,"Can only delete unsold product");
            return productsRepo.delete(product).thenReturn(product);
        }).doOnSuccess(savedProduct ->log.info("Update Product with Id : {}",savedProduct.getId()))
                .doOnError(e-> log.error("Error : {}",e.getMessage()));
    }

    public Mono<Product> updateProductStatus(String productId, ProductStatus newStatus) {
        Mono<Product> productMono = findProduct(productId);
        return productMono.flatMap(product -> {
            Assert.isTrue(product.getStatus()!=null && !product.getStatus().equals(ProductStatus.SOLD),"Product is already sold");
            if(newStatus.equals(ProductStatus.SOLD) && product.getQuantity()!=0)
                return Mono.error(new RuntimeException("Can only update to Sold id Quantity is zero"));
            product.setStatus(newStatus);
            return productsRepo.save(product);
        }).doOnSuccess(savedProduct ->log.info("Update Product with Id : {}",savedProduct.getId()))
                .doOnError(e-> log.error("Error : {}",e.getMessage()));
    }

    public Flux<Product> findAllByID(List<String> productId){
        return productsRepo.findAllById(productId)
                .collectList().flatMapMany(products -> {
                    Set<String> dbProductId   = products.stream().map(Product::getId).collect(Collectors.toSet());
                    Set<String> userProductId = new HashSet<>(productId);
                   if(dbProductId.size()!=userProductId.size() || !userProductId.equals(dbProductId)){
                       return Flux.error(new RuntimeException("Few Product Id in input are invalid"));
                   }
                    return Flux.fromIterable(products);
                })
                .switchIfEmpty(Flux.error(new RuntimeException("No Product with given Id's found")));
    }


    public Flux<Product> findAllByIdAndProductStatus(List<String> productId){
        return productsRepo.findAllByIdInAndStatus(productId,ProductStatus.UNSOLD)
                .collectList().flatMapMany(products -> {
                    Set<String> dbProductId   = products.stream().map(Product::getId).collect(Collectors.toSet());
                    Set<String> userProductId = new HashSet<>(productId);
                    if(dbProductId.size()!=userProductId.size() || !userProductId.equals(dbProductId)){
                        throw new RuntimeException("FEW PRODUCT IN THE ORDER ARE SOLD/ RETURNED PLEASE UPDATE THE ORDER");
                    }
                    return Flux.fromIterable(products);
                })
                .switchIfEmpty(Flux.error(new RuntimeException("No Product with given Id's found unsold")));
    }


}
