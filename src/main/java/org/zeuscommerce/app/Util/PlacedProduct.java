package org.zeuscommerce.app.Util;


import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder
public class PlacedProduct {

    String productId;
    Integer quantity;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlacedProduct that = (PlacedProduct) o;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

}
