package org.meveo.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a single CF simple value inside a more complex CF value (list, map, matrix)
 * 
 * @author Andrius Karpavicius
 **/
@XmlRootElement(name = "CustomFieldValue")
@XmlAccessorType(XmlAccessType.FIELD)
public class CustomFieldValueDto implements Serializable {

	private static final long serialVersionUID = -6551785257592739335L;

	@XmlElements({ @XmlElement(name = "dateValue", type = Date.class), @XmlElement(name = "doubleValue", type = Double.class), @XmlElement(name = "longValue", type = Long.class),
			@XmlElement(name = "stringValue", type = String.class), @XmlElement(name = "entityReferenceValue", type = EntityReferenceDto.class),
			@XmlElement(name = "childEntityValue", type = CustomEntityInstanceDto.class) })
	protected Object value;

	public CustomFieldValueDto() {
	}

	private Object fromDTO() {

		if (value instanceof EntityReferenceDto) {
			return ((EntityReferenceDto) value).fromDTO();
		} else {
			return value;
		}
	}

	public static List<Object> fromDTO(List<CustomFieldValueDto> listValue) {
		List<Object> values = new ArrayList<Object>();
		for (CustomFieldValueDto valueDto : listValue) {
			values.add(valueDto.fromDTO());
		}
		return values;
	}

	public static LinkedHashMap<String, Object> fromDTO(Map<String, CustomFieldValueDto> mapValue) {
		LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, CustomFieldValueDto> valueDto : mapValue.entrySet()) {
			values.put(valueDto.getKey(), valueDto.getValue().fromDTO());
		}
		return values;
	}

	public CustomFieldValueDto(Object e) {
		this.value = e;
	}

	@Override
	public String toString() {
		return String.format("CustomFieldValueDto [value=%s]", value);
	}

	/**
	 * Check if value is empty
	 * 
	 * @return True if value is empty
	 */
	public boolean isEmpty() {
		if (value == null) {
			return true;
		}
		if (value instanceof EntityReferenceDto) {
			return ((EntityReferenceDto) value).isEmpty();
		} else if (value instanceof String) {
			return ((String) value).length() == 0;
		}
		return false;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}
}