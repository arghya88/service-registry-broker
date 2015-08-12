package org.cf.servicebroker.model;

import javax.persistence.*;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name="services")
public class Service {

	@Id
	private String id;
	
	@Column(nullable = false)
	private String name;
	
	@Column(nullable = false)
	private String description;
	
	@Column(nullable = false)
	private boolean bindable;
	
	@OneToMany(orphanRemoval = true)
	@JoinColumn( name = "service_id")
	private Set<Plan> plans = new HashSet<Plan>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String descrp) {
		this.description = descrp;
	}

	public boolean isBindable() {
		return bindable;
	}

	public void setBindable(boolean bindable) {
		this.bindable = bindable;
	}

	public Set<Plan> getPlans() {
		return plans;
	}

	public void setPlans(Set<Plan> plans) {
		this.plans = plans;
	}
	
	public void addPlan(Plan plan) {
		this.plans.add(plan);
	}
	
}
