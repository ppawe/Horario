package hft.wiinf.de.horario.controller;

import com.activeandroid.query.Select;

import java.util.List;

import hft.wiinf.de.horario.model.Person;

/**
 * Class with various methods related to saving, deleting and retrieving Person objects to and from the database
 */
public class PersonController {

    /**
     * adds a new {@link Person} to the database if a person with the same phoneNumber doesn't exist already
     *
     * @param phoneNumber the phone number of the new person
     * @param name        the name of the new Person
     * @return either the newly created person or the preexisting person
     */
    public static Person addOrGetPerson(String phoneNumber, String name) {
        if (getPersonViaPhoneNumber(phoneNumber) == null) {
            Person person = new Person(phoneNumber, name);
            person.save();
            return person;
        }
        return getPersonViaPhoneNumber(phoneNumber);
    }

    /**
     * gets the {@link Person} with the given Id from the database
     *
     * @param id the id of the Person to be retrieved
     * @return the Person with the given Id
     */
    public static Person getPersonById(long id) {
        return (Person) new Select().from(Person.class).where("id = ?", id).executeSingle();
    }

    /**
     *
     * @return the {@link Person} representing the user of the app
     */
    public static Person getPersonWhoIam() {
        return new Select()
                .from(Person.class)
                .where("isItMe = ?", true)
                .executeSingle();
    }

    /**
     * gets the {@link Person} with the given phone number
     * @param phoneNumber the phone number of the Person (should be in E.164 format)
     * @return the person with the given phone number
     */
    public static Person getPersonViaPhoneNumber(String phoneNumber) {
        return new Select()
                .from(Person.class)
                .where("phoneNumber = ?", phoneNumber)
                .executeSingle();
    }


    /**
     *
     * @return a list of all saved {@link Person} Objects
     */
    public static List<Person> getAllPersons() {
        return new Select()
                .from(Person.class)
                .execute();
    }

    /**
     * saves the given {@link Person} to the database
     * @param person the person to be saved
     */
    public static void savePerson(Person person) {
        if (person != null) {
            person.save();
        }
    }

    /**
     * deletes the given {@link Person} from the database
     * @param person the person to be deleted
     */
    public static void deletePerson(Person person) {
        if (person != null && person.getId() != null) {
            person.delete();
        }
    }
}