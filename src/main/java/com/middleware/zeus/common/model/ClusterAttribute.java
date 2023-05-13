package com.middleware.zeus.common.model;


import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author damiao
 * @date 2020/12/16 4:59 PM
 */
@Accessors(chain = true)
@Data
@ApiModel(description = "Cluster属性列表")
public class ClusterAttribute implements Serializable {

	private static final long serialVersionUID = -6027103686869949521L;
	private String name;
	private Object value;

}
