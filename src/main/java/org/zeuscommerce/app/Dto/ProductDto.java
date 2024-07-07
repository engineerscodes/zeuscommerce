package org.zeuscommerce.app.Dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import org.zeuscommerce.app.Util.ProductStatus;

import java.util.Map;


@Data
@Builder
public class ProductDto {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @DecimalMin(value = "0",message = "Price can't be negative")
    private double price;


    private Map<String, Object> metaData ;


    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private double longitude;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Min(value = 0,message = "quality can't be less than Zero")
    @Max(value = 100,message = "Max 100 only possible")
    private Integer quantity;
}
