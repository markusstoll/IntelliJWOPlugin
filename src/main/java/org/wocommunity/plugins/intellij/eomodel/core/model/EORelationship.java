package org.wocommunity.plugins.intellij.eomodel.core.model;

import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EORelationship {
	private EOEntity entity;
	private String name;
	private String destinationEntityName;
	private EOEntity destination;

	private boolean toMany = false;
	private boolean mandatory = false;
	private EODeleteRule deleteRule = EODeleteRule.NULLIFY;
	private EOJoinSemantic joinSemantic = EOJoinSemantic.INNER;
	private final List<EOJoin> joins = new ArrayList<>();

	private String definition;
	private boolean ownsDestination = false;
	private boolean propagatesPrimaryKey = false;
	private Integer batchFetchCount;

	private boolean classProperty = true;
	private boolean clientClassProperty = false;

	private final Map<Object, Object> userInfo = new PropertyListMap<>();
	private final Map<Object, Object> internalInfo = new PropertyListMap<>();
	private final Map<Object, Object> rawProperties = new PropertyListMap<>();

	public EORelationship() {
	}

	public EORelationship(String name) {
		this.name = name;
	}

	public EOEntity getEntity() {
		return entity;
	}

	public void setEntity(EOEntity entity) {
		this.entity = entity;
	}

	public String getName() {
		return name;
	}

	public String getUppercaseUnderscoreName() {
		if (name == null) return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c) && i > 0 && Character.isLowerCase(name.charAt(i - 1))) {
				sb.append('_');
			}
			sb.append(Character.toUpperCase(c));
		}
		return sb.toString();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDestinationEntityName() {
		return destinationEntityName;
	}

	public void setDestinationEntityName(String destinationEntityName) {
		this.destinationEntityName = destinationEntityName;
	}

	public EOEntity getDestination() {
		return destination;
	}

	public void setDestination(EOEntity destination) {
		this.destination = destination;
		if (destination != null) {
			this.destinationEntityName = destination.getName();
		}
	}

	public boolean isToMany() {
		return toMany;
	}

	public void setToMany(boolean toMany) {
		this.toMany = toMany;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public EODeleteRule getDeleteRule() {
		return deleteRule;
	}

	public void setDeleteRule(EODeleteRule deleteRule) {
		this.deleteRule = deleteRule != null ? deleteRule : EODeleteRule.NULLIFY;
	}

	public EOJoinSemantic getJoinSemantic() {
		return joinSemantic;
	}

	public void setJoinSemantic(EOJoinSemantic joinSemantic) {
		this.joinSemantic = joinSemantic != null ? joinSemantic : EOJoinSemantic.INNER;
	}

	public List<EOJoin> getJoins() {
		return joins;
	}

	public void addJoin(EOJoin join) {
		if (join != null) {
			join.setRelationship(this);
			joins.add(join);
		}
	}

	public void removeJoin(EOJoin join) {
		joins.remove(join);
	}

	public void clearJoins() {
		joins.clear();
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public boolean isFlattened() {
		return definition != null && !definition.trim().isEmpty();
	}

	public boolean isOwnsDestination() {
		return ownsDestination;
	}

	public void setOwnsDestination(boolean ownsDestination) {
		this.ownsDestination = ownsDestination;
	}

	public boolean isPropagatesPrimaryKey() {
		return propagatesPrimaryKey;
	}

	public void setPropagatesPrimaryKey(boolean propagatesPrimaryKey) {
		this.propagatesPrimaryKey = propagatesPrimaryKey;
	}

	public Integer getBatchFetchCount() {
		return batchFetchCount;
	}

	public void setBatchFetchCount(Integer batchFetchCount) {
		this.batchFetchCount = batchFetchCount;
	}

	public boolean isClassProperty() {
		return classProperty;
	}

	public void setClassProperty(boolean classProperty) {
		this.classProperty = classProperty;
	}

	public boolean isClientClassProperty() {
		return clientClassProperty;
	}

	public void setClientClassProperty(boolean clientClassProperty) {
		this.clientClassProperty = clientClassProperty;
	}

	public Map<Object, Object> getUserInfo() {
		return userInfo;
	}

	public Map<Object, Object> getInternalInfo() {
		return internalInfo;
	}

	public Map<Object, Object> getRawProperties() {
		return rawProperties;
	}

	public void resolve(EOModel model) {
		if (model != null && destinationEntityName != null) {
			this.destination = model.getEntity(destinationEntityName);
		}
		for (EOJoin join : joins) {
			join.resolve(this.entity, this.destination);
		}
	}

	public void loadFromMap(Map<?, ?> map) {
		rawProperties.clear();
		if (map == null) return;
		rawProperties.putAll(map);
		EOModelMap m = new EOModelMap(map);

		this.name = m.getString("name", true);
		this.destinationEntityName = m.getString("destination", true);
		this.definition = m.getString("definition", true);

		this.toMany = m.getBoolean("isToMany", false);
		this.mandatory = m.getBoolean("isMandatory", false);
		this.ownsDestination = m.getBoolean("ownsDestination", false);
		this.propagatesPrimaryKey = m.getBoolean("propagatesPrimaryKey", false);
		this.batchFetchCount = m.getInteger("numberOfToManyFaultsToBatchFetch");

		String delRuleStr = m.getString("deleteRule", true);
		this.deleteRule = EODeleteRule.fromPlistName(delRuleStr);

		String joinSemStr = m.getString("joinSemantic", true);
		this.joinSemantic = EOJoinSemantic.fromPlistName(joinSemStr);

		joins.clear();
		List<?> rawJoins = m.getList("joins");
		if (rawJoins != null) {
			for (Object item : rawJoins) {
				if (item instanceof Map) {
					EOJoin join = new EOJoin();
					join.setRelationship(this);
					join.loadFromMap((Map<?, ?>) item);
					joins.add(join);
				}
			}
		}

		Map<?, ?> userInfoMap = m.getMap("userInfo");
		if (userInfoMap != null) {
			this.userInfo.clear();
			this.userInfo.putAll(userInfoMap);
		}

		Map<?, ?> internalInfoMap = m.getMap("internalInfo");
		if (internalInfoMap != null) {
			this.internalInfo.clear();
			this.internalInfo.putAll(internalInfoMap);
		}
	}

	public EOModelMap toMap() {
		EOModelMap map = new EOModelMap(new PropertyListMap<>(rawProperties));
		map.setString("name", name, true);
		map.setString("destination", destinationEntityName, true);
		map.setString("definition", definition, true);

		map.setBoolean("isToMany", toMany, EOModelMap.BooleanStyle.Y_N_OPTIONAL_DEFAULT_NO);
		map.setBoolean("isMandatory", mandatory, EOModelMap.BooleanStyle.Y_N_OPTIONAL_DEFAULT_NO);
		map.setBoolean("ownsDestination", ownsDestination, EOModelMap.BooleanStyle.Y_N_OPTIONAL_DEFAULT_NO);
		map.setBoolean("propagatesPrimaryKey", propagatesPrimaryKey, EOModelMap.BooleanStyle.Y_N_OPTIONAL_DEFAULT_NO);
		map.setInteger("numberOfToManyFaultsToBatchFetch", batchFetchCount);

		if (deleteRule != null && deleteRule != EODeleteRule.NULLIFY) {
			map.setString("deleteRule", deleteRule.getPlistName());
		} else if (deleteRule == EODeleteRule.NULLIFY) {
			map.setString("deleteRule", deleteRule.getPlistName());
		}

		if (joinSemantic != null && joinSemantic != EOJoinSemantic.INNER) {
			map.setString("joinSemantic", joinSemantic.getPlistName());
		} else if (joinSemantic == EOJoinSemantic.INNER) {
			map.setString("joinSemantic", joinSemantic.getPlistName());
		}

		if (!joins.isEmpty()) {
			List<Map<String, Object>> joinsList = new ArrayList<>();
			for (EOJoin join : joins) {
				joinsList.add(join.toMap());
			}
			map.setList("joins", joinsList);
		} else {
			map.remove("joins");
		}

		if (!userInfo.isEmpty()) {
			map.setMap("userInfo", userInfo);
		} else {
			map.remove("userInfo");
		}

		if (!internalInfo.isEmpty()) {
			map.setMap("internalInfo", internalInfo);
		} else {
			map.remove("internalInfo");
		}

		return map;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EORelationship that = (EORelationship) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}

	@Override
	public String toString() {
		return name != null ? name : super.toString();
	}
}

