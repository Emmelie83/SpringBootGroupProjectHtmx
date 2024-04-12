package se.iths.springbootgroupproject.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDate;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @CreatedDate
    private LocalDate createdDate;

    @Setter
    @LastModifiedDate
    @jakarta.persistence.Column(insertable = false)
    private LocalDate lastModifiedDate;

    @Setter
    @LastModifiedBy
    @jakarta.persistence.Column(insertable = false)
    private String lastModifiedBy;

    @Setter
    @Column("title")
    private String messageTitle;

    @Setter
    @Column("message_body")
    private String messageBody;

    @Setter
    @Column("message_language")
    private String messageLanguage;

    @Setter
    @ManyToOne()
    @JoinColumn(name = "user")
    private User user;

    @Setter
    @Column("is_public")
    private boolean isPublic = false;


}
