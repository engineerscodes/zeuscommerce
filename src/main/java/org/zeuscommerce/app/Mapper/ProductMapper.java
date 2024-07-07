package org.zeuscommerce.app.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.zeuscommerce.app.Dto.ProductDto;
import org.zeuscommerce.app.Entity.Product;



@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductMapper instance = Mappers.getMapper(ProductMapper.class);

    Product ProductDtoToProduct(ProductDto p);

    ProductDto ProductToProductDto(Product p);

    @Mapping(source = "updateProduct.status",target ="status")
    void UpdateProduct(ProductDto updateProduct, @MappingTarget Product existingProduct);

}
