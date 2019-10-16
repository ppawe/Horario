package hft.wiinf.de.horario.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * many to many relation to Event in EventPerson table
 * enable push, notification time, start tab should only be read out for the app user
 *
 * @// TODO: 14.10.19  if you find the time just put those in a config file or something that's 3 columns full of wasted space
 */
//
@Table(name = "persons")
public class Person extends Model {
    @Column(unique = true)
    private String phoneNumber = "";
    @Column
    private String name = "";
    @Column
    private boolean isItMe = false;
    @Column
    private boolean enablePush = true;
    @Column
    private int notificationTime = 15;
    @Column
    private int startTab = 1;


    /**
     * Instantiates a new Person. Use this constructor for person that is this app's user
     *
     * @param isItMe      if the person is the app user
     * @param phoneNumber the phone number of the user
     * @param name        the name of ther user
     */
    public Person(boolean isItMe, String phoneNumber, String name) {
        super();
        this.isItMe = isItMe;
        this.phoneNumber = phoneNumber;
        this.name = name;
    }

    /**
     * Instantiates a new Person. Use this constructor for people that are not the current user of this app
     *
     * @param phoneNumber the phone number of the user
     * @param name        the name of the user
     */
    public Person(String phoneNumber, String name) {
        super();
        this.isItMe = false;
        this.phoneNumber = phoneNumber;
        this.name = name;
    }

    /**
     * Instantiates a new Person.
     *
     * @param phoneNumber      the phone number of the user
     * @param notificationTime the notification time in minutes before the event
     */
    public Person(String phoneNumber, int notificationTime) {
        super();
        this.phoneNumber = phoneNumber;
        this.notificationTime = notificationTime;
    }

    /**
     * Instantiates a new Person.
     */
    public Person() {
        super();
    }

    /**
     * get If the person is the app user
     *
     * @return If the person is the app user
     */
    public boolean isItMe() {
        return isItMe;
    }

    /**
     * set If the person is the app user
     *
     * @param itMe If the person is the app user
     */
    public void setItMe(boolean itMe) {
        this.isItMe = itMe;
    }

    /**
     * Gets the phone number of the user. the number is only a string, and no specific format - this should be ensured by the application
     *
     * @return the phone number of the user
     */
    //getter-setter
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * sets the phone number of the user. The number is only a string, and no specific format - this should be ensured by the application
     *
     * @param phoneNumber the phone number of the user. The number is only a string, and no specific format - this should be ensured by the application
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }


    /**
     * Gets the name of the user
     *
     * @return the name of the user
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the user.
     *
     * @param name the name of the user
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Gets notification time of this user.
     *
     * @return the notification time in minutes before the event
     */
    public int getNotificationTime() {
        return notificationTime;
    }

    /**
     * Sets notification time.
     *
     * @param notificationTime the notification time in minutes before the event
     */
    public void setNotificationTime(int notificationTime) {
        this.notificationTime = notificationTime;
    }

    /**
     * If push is enabled
     *
     * @return if push is enabled
     */
    public boolean isEnablePush() {
        return enablePush;
    }

    /**
     * Set if push is enabled
     *
     * @param enablePush if push is enabled
     */
    public void setEnablePush(boolean enablePush) {
        this.enablePush = enablePush;
    }

    /**
     * Gets the start tab of the user.
     *
     * @return the start tab of the user (0 based from left (0) to right (2))
     */
    public int getStartTab() {
        return startTab;
    }

    /**
     * Sets the start tab of the user.
     *
     * @param startTab the start tab of the user (0 based from left (0) to right (2)
     */
    public void setStartTab(int startTab) {
        this.startTab = startTab;
    }

    /**
     * Gets rejection reason.
     *
     * @return the rejection reason of the user
     */

}


