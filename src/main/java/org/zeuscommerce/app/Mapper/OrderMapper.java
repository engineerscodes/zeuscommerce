package org.zeuscommerce.app.Mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.zeuscommerce.app.Dto.OrderDto;
import org.zeuscommerce.app.Entity.Order;



@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderMapper instance = Mappers.getMapper(OrderMapper.class);

    void OrderDtoToOrder(OrderDto o, @MappingTarget Order order );
}
