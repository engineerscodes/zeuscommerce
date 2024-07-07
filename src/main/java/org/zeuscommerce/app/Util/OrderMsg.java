package org.zeuscommerce.app.Util;


import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.util.Map;

@Data
@Builder
public class OrderMsg {

    @Id
    private String id;

    @Version
    private Long OrderVersion;

    Map<String,Map<String,Long>> productVersion;

}
