package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import io.jsonwebtoken.lang.Collections;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.util.List;

@Data
public class SpuAttrValueVo extends SpuAttrValueEntity {
    private List<String> valueSelected;

    public void setValueSelected(List<String> valueSelected) {
        if (Collections.isEmpty(valueSelected)){
            return;
        }
        setAttrValue(StringUtils.join(valueSelected,","));
    }
}
