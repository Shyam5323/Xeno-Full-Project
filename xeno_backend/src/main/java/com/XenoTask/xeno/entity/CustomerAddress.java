package com.xenotask.xeno.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_addresses", indexes = {
        @Index(name = "idx_address_customer", columnList = "customer_id"),
        @Index(name = "idx_address_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "shopify_address_id")
    private Long shopifyAddressId;

    @Column(name = "address1")
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "zip", length = 20)
    private String zip;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = Boolean.FALSE;
}
