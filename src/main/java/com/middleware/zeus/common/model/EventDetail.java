package com.middleware.zeus.common.model;

import java.util.Date;

import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
public class EventDetail {
	
	private String reason;
	
	private String message;
	
	private Date firstTimestamp;
	
	private Date lastTimestamp;
	
	private Integer count;
	
	private String type;
	
	private ObjectReference involvedObject;
	
	private Long span;
	
	private String spanMetric;

	private Date eventTime;

	private String eventExplain;

	private boolean isMiddleware;

	private String chartName;

	private String chartVersion;

	public EventDetail() {

	}

}
