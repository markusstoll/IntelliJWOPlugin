package org.wocommunity.plugins.intellij.eomodel.core.model;

import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListMap;

import java.util.Map;
import java.util.Objects;

public class EOJoin {
	private EORelationship relationship;
	private String sourceAttributeName;
	private String destinationAttributeName;
	private EOAttribute sourceAttribute;
	private EOAttribute destinationAttribute;

	public EOJoin() {
	}

	public EOJoin(String sourceAttributeName, String destinationAttributeName) {
		this.sourceAttributeName = sourceAttributeName;
		this.destinationAttributeName = destinationAttributeName;
	}

	public EOJoin(EOAttribute sourceAttribute, EOAttribute destinationAttribute) {
		this.sourceAttribute = sourceAttribute;
		this.destinationAttribute = destinationAttribute;
		if (sourceAttribute != null) {
			this.sourceAttributeName = sourceAttribute.getName();
		}
		if (destinationAttribute != null) {
			this.destinationAttributeName = destinationAttribute.getName();
		}
	}

	public EORelationship getRelationship() {
		return relationship;
	}

	public void setRelationship(EORelationship relationship) {
		this.relationship = relationship;
	}

	public String getSourceAttributeName() {
		return sourceAttributeName;
	}

	public void setSourceAttributeName(String sourceAttributeName) {
		this.sourceAttributeName = sourceAttributeName;
	}

	public String getDestinationAttributeName() {
		return destinationAttributeName;
	}

	public void setDestinationAttributeName(String destinationAttributeName) {
		this.destinationAttributeName = destinationAttributeName;
	}

	public EOAttribute getSourceAttribute() {
		return sourceAttribute;
	}

	public void setSourceAttribute(EOAttribute sourceAttribute) {
		this.sourceAttribute = sourceAttribute;
		if (sourceAttribute != null) {
			this.sourceAttributeName = sourceAttribute.getName();
		}
	}

	public EOAttribute getDestinationAttribute() {
		return destinationAttribute;
	}

	public void setDestinationAttribute(EOAttribute destinationAttribute) {
		this.destinationAttribute = destinationAttribute;
		if (destinationAttribute != null) {
			this.destinationAttributeName = destinationAttribute.getName();
		}
	}

	public void resolve(EOEntity sourceEntity, EOEntity destinationEntity) {
		if (sourceEntity != null && sourceAttributeName != null) {
			this.sourceAttribute = sourceEntity.getAttribute(sourceAttributeName);
		}
		if (destinationEntity != null && destinationAttributeName != null) {
			this.destinationAttribute = destinationEntity.getAttribute(destinationAttributeName);
		}
	}

	public void loadFromMap(Map<?, ?> map) {
		if (map == null) return;
		Object src = map.get("sourceAttribute");
		if (src != null) {
			this.sourceAttributeName = src.toString();
		}
		Object dst = map.get("destinationAttribute");
		if (dst != null) {
			this.destinationAttributeName = dst.toString();
		}
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new PropertyListMap<>();
		if (destinationAttributeName != null) {
			map.put("destinationAttribute", destinationAttributeName);
		}
		if (sourceAttributeName != null) {
			map.put("sourceAttribute", sourceAttributeName);
		}
		return map;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EOJoin eoJoin = (EOJoin) o;
		return Objects.equals(sourceAttributeName, eoJoin.sourceAttributeName) &&
				Objects.equals(destinationAttributeName, eoJoin.destinationAttributeName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sourceAttributeName, destinationAttributeName);
	}

	@Override
	public String toString() {
		return (sourceAttributeName != null ? sourceAttributeName : "?") + " -> " +
				(destinationAttributeName != null ? destinationAttributeName : "?");
	}
}

