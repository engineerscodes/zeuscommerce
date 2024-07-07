package org.zeuscommerce.app.Controller;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.zeuscommerce.app.Dto.ProductDto;
import org.zeuscommerce.app.Entity.Product;
import org.zeuscommerce.app.Service.ProductService;
import org.zeuscommerce.app.Util.ProductStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/products")
public class ProductController {


    @Autowired
    ProductService productService;




    @GetMapping
    //@Cacheable(value = "Products",sync = true)
    public Flux<Product> getProducts(){
        return productService.getProducts();
        // pagination need ask page size
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Object saveNewProduct(@Valid @RequestBody ProductDto p){
        try {
            return productService.saveNewProduct(p);
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping("/{productId}")
    public Object getProduct(@PathVariable String productId){
        return productService.findProduct(productId)
                .map(ResponseEntity::ok)
                .cast(ResponseEntity.class)
                .onErrorResume(e->Mono.just(ResponseEntity.badRequest().body("No product for given id")));
    }


    @PutMapping("/{productId}")
    public Mono<ResponseEntity<String>> updateProduct(@PathVariable String productId,@Valid @RequestBody ProductDto p){
            return productService.updateProduct(productId, p)
                    .map(product -> ResponseEntity.ok(String.format("Product updated successfully with id : %s",product.getId())))
                    .onErrorResume(e-> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @DeleteMapping("/{productId}")
    public Mono<ResponseEntity<String>> DeleteProduct(@PathVariable String productId){
        return productService.deleteProduct(productId).
                map(product -> ResponseEntity.ok(String.format("Deleted Product with id : %s",product.getId())))
                .onErrorResume(e-> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }


    @PutMapping("/{productId}/updateStatus")
    public Object updateProductStatus(@PathVariable String productId, @RequestBody ProductStatus newStatus){
       return productService.updateProductStatus(productId,newStatus).
               map(product -> ResponseEntity.ok(String.format("Updated Product with id : %s to status : %s",product.getId(),product.getStatus())))
               .onErrorResume(e-> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }


}
