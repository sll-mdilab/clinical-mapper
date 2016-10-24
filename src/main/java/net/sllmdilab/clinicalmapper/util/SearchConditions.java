package net.sllmdilab.clinicalmapper.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sllmdilab.commons.util.T5FHIRUtils;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

public class SearchConditions {
	private List<String> conditions = new ArrayList<String>();

	public SearchConditions addUndefined(String variable) {
		if (variable != null) {
			conditions.add(" not ( " + variable + " ) ");
		}

		return this;
	}

	public SearchConditions addEquals(String variable, String value) {
		addEquals(variable, value, "");

		return this;
	}

	public SearchConditions addEquals(String variable, String value, String prefix) {
		if (value != null) {
			conditions.add(variable + " = \"" + prefix + StringEscapeUtils.escapeXml10(value) + "\"");
		}
		return this;
	}

	public SearchConditions addEquals(String variable, Date value) {
		add(variable, "=", value);

		return this;
	}

	public SearchConditions add(String variable, String operator, Date value) {
		if (value != null) {
			conditions.add(variable + " " + operator + " \"" + T5FHIRUtils.convertDateToXMLType(value) + "\"");
		}
		return this;
	}

	public SearchConditions addOrUndefined(String variable, String operator, Date value) {
		if (value != null) {
			conditions.add("( " + variable + " " + operator + " \"" + T5FHIRUtils.convertDateToXMLType(value)
					+ "\" or not ( " + variable + ") ) ");
		}
		return this;
	}

	@Override
	public String toString() {
		if (conditions.isEmpty()) {
			return "";
		} else {
			return "[" + StringUtils.join(conditions, " and ") + "]";
		}
	}
}
