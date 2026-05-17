package com.marketplace.delivery.domain;

/**
 * Lifecycle states of a parcel in the delivery system.
 * Ordered to naturally follow the delivery flow.
 * Designed so a future ML service can use these states as features.
 */
public enum DeliveryStatus {

    /** Parcel record created, awaiting hub assignment. */
    CREATED,

    /** A regional hub has been assigned to this parcel. */
    ASSIGNED_TO_HUB,

    /** Parcel has been sorted at the hub by destination zone. */
    SORTED,

    /** Parcel is in transit from hub to relay point. */
    IN_TRANSIT,

    /** Parcel has arrived at the relay point. */
    ARRIVED_AT_RELAY,

    /** Parcel is out for last-mile delivery. */
    OUT_FOR_DELIVERY,

    /** Parcel successfully delivered to recipient. */
    DELIVERED,

    /** Delivery attempt failed (recipient absent, wrong address, etc.). */
    FAILED,

    /** Parcel returned to sender after failed delivery attempt(s). */
    RETURNED
}
