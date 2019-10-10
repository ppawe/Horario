package hft.wiinf.de.horario.model;


/**
 *Defines the state of the event, if it is rejected, accepted or waiting (saved but with no feedback) or invited
 */
public enum AcceptedState {
    REJECTED,
    WAITING,
    ACCEPTED,
    INVITED
}
