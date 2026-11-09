package org.wocommunity.plugins.intellij.eomodel.core.model;

import java.util.ArrayList;
import java.util.List;

public class EOModelGroup {
	private final List<EOModel> models = new ArrayList<>();

	public List<EOModel> getModels() {
		return models;
	}

	public void addModel(EOModel model) {
		if (model != null && !models.contains(model)) {
			models.add(model);
			model.setModelGroup(this);
		}
	}

	public EOEntity getEntityNamed(String entityName) {
		if (entityName == null) return null;
		for (EOModel model : models) {
			for (EOEntity entity : model.getEntities()) {
				if (entityName.equals(entity.getName())) {
					return entity;
				}
			}
		}
		return null;
	}
}
