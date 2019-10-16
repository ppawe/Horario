package hft.wiinf.de.horario.model;


/**
 * Defines the state of the {@link EventPerson} relationship, whether the person has
 * rejected or accepted the event or has not responded(saved the event but with no feedback) or is only invited
 */
public enum AcceptedState {
    REJECTED,
    WAITING,
    ACCEPTED,
    INVITED
}
