/********************************************************************************************************
 * File:  MedicalSchool.java Course Materials CST 8277
 *
 * @author Teddy Yap
 * 
 */
package acmemedical.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * The persistent class for the medical_school database table.
 */
@Entity
@Table(name = "medical_school")
@AttributeOverride(name = "id", column = @Column(name = "school_id"))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "public", length = 1, discriminatorType = DiscriminatorType.INTEGER)
@NamedQueries({
    @NamedQuery(
        name = MedicalSchool.ALL_MEDICAL_SCHOOLS_QUERY_NAME,
        query = "SELECT distinct ms FROM MedicalSchool ms LEFT JOIN FETCH ms.medicalTrainings"
    ),
    @NamedQuery(
        name = MedicalSchool.SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME,
        query = "SELECT ms FROM MedicalSchool ms WHERE ms.id = :param1"
    ),
    @NamedQuery(
        name = MedicalSchool.IS_DUPLICATE_QUERY_NAME,
        query = "SELECT count(ms) FROM MedicalSchool ms WHERE ms.name = :param1"
    )
})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PrivateSchool.class, name = "private_school"),
    @JsonSubTypes.Type(value = PublicSchool.class, name = "public_school")
})
public abstract class MedicalSchool extends PojoBase implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ALL_MEDICAL_SCHOOLS_QUERY_NAME = "MedicalSchool.findAll";
    public static final String SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME = "MedicalSchool.findById";
    public static final String IS_DUPLICATE_QUERY_NAME = "MedicalSchool.isDuplicate";

    @Column
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "school")
    private Set<MedicalTraining> medicalTrainings = new HashSet<>();

    @Transient
    private boolean isPublic;

    public MedicalSchool() {
        super();
    }

    public MedicalSchool(boolean isPublic) {
        this();
        this.isPublic = isPublic;
    }

    @JsonIgnore
    public Set<MedicalTraining> getMedicalTrainings() {
        return medicalTrainings;
    }

    public void setMedicalTrainings(Set<MedicalTraining> medicalTrainings) {
        this.medicalTrainings = medicalTrainings;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        return prime * result + Objects.hash(getId(), getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }

        if (obj instanceof MedicalSchool otherMedicalSchool) {
            return Objects.equals(this.getId(), otherMedicalSchool.getId()) &&
                   Objects.equals(this.getName(), otherMedicalSchool.getName());
        }
        return false;
    }


    @Override
    public String toString() {
        return "MedicalSchool [id=" + getId() + ", name=" + name + ", isPublic=" + isPublic + "]";
    }
}
