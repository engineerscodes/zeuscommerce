package org.zeuscommerce.app.Dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Version;
import org.zeuscommerce.app.Util.OrderStatus;
import org.zeuscommerce.app.Util.PlacedProduct;

import java.util.Map;
import java.util.Set;

@Data
@Builder
public class OrderDto {

    private Map<String, Object> orderDetails; // store user data & location

    Set<PlacedProduct> placedProducts;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private boolean rushOrder;


}
