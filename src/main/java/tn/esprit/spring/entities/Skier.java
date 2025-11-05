package tn.esprit.spring.entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Set;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Skier implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long numSkier;

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String city;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private Subscription subscription;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "excursion",
            joinColumns = @JoinColumn(name = "numSkier"),
            inverseJoinColumns = @JoinColumn(name = "numPiste"))
    private Set<Piste> pistes;

    @OneToMany(mappedBy = "skier")
    private Set<Registration> registrations;
}
