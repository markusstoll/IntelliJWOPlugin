package org.wocommunity.plugins.intellij.eomodel.core.model;

import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListMap;

import java.util.Map;
import java.util.Objects;

public class EOConnectionDictionary {
	private String url;
	private String driver;
	private String username;
	private String password;
	private String plugin;
	private final Map<Object, Object> rawProperties = new PropertyListMap<>();

	public EOConnectionDictionary() {
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPlugin() {
		return plugin;
	}

	public void setPlugin(String plugin) {
		this.plugin = plugin;
	}

	public Map<Object, Object> getRawProperties() {
		return rawProperties;
	}

	public void loadFromMap(Map<?, ?> map) {
		rawProperties.clear();
		if (map == null) return;
		rawProperties.putAll(map);

		Object urlObj = map.get("URL");
		if (urlObj == null) urlObj = map.get("url");
		this.url = urlObj != null ? urlObj.toString() : null;

		Object driverObj = map.get("driver");
		this.driver = driverObj != null ? driverObj.toString() : null;

		Object userObj = map.get("username");
		if (userObj == null) userObj = map.get("user");
		this.username = userObj != null ? userObj.toString() : null;

		Object passObj = map.get("password");
		this.password = passObj != null ? passObj.toString() : null;

		Object pluginObj = map.get("plugin");
		this.plugin = pluginObj != null ? pluginObj.toString() : null;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new PropertyListMap<>();
		for (Map.Entry<Object, Object> entry : rawProperties.entrySet()) {
			if (entry.getKey() != null) {
				map.put(entry.getKey().toString(), entry.getValue());
			}
		}
		if (url != null && !url.trim().isEmpty()) {
			map.put("URL", url);
		} else {
			map.remove("URL");
			map.remove("url");
		}
		if (driver != null && !driver.trim().isEmpty()) {
			map.put("driver", driver);
		} else {
			map.remove("driver");
		}
		if (username != null && !username.trim().isEmpty()) {
			map.put("username", username);
		} else {
			map.remove("username");
			map.remove("user");
		}
		if (password != null && !password.trim().isEmpty()) {
			map.put("password", password);
		} else {
			map.remove("password");
		}
		if (plugin != null && !plugin.trim().isEmpty()) {
			map.put("plugin", plugin);
		} else {
			map.remove("plugin");
		}
		return map;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EOConnectionDictionary that = (EOConnectionDictionary) o;
		return Objects.equals(url, that.url) &&
				Objects.equals(driver, that.driver) &&
				Objects.equals(username, that.username) &&
				Objects.equals(password, that.password) &&
				Objects.equals(plugin, that.plugin);
	}

	@Override
	public int hashCode() {
		return Objects.hash(url, driver, username, password, plugin);
	}
}

