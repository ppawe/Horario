package hft.wiinf.de.horario.controller;

import android.util.Log;

import com.activeandroid.query.Select;

import java.util.List;

import hft.wiinf.de.horario.model.Person;

public class PersonController {

    public static void addPersonMe(Person person) {
        try {
            PersonController.savePerson(person);
        } catch (Exception e) {
            Log.d("PersonController", "addPersonMe:" + e.getMessage());
        }
    }

    public static Person addPerson(String phoneNumber, String name) {
        if (getPersonViaPhoneNumber(phoneNumber) == null) {
            Person person = new Person(phoneNumber, name);
            person.save();
            return person;
        }
        return getPersonViaPhoneNumber(phoneNumber);
    }
    public static Person getPersonById(long id) {
        return (Person) new Select().from(Person.class).where("id = ?", id).executeSingle();
    }
    public static Person getPersonWhoIam() {
        return new Select()
                .from(Person.class)
                .where("isItMe = ?", true)
                .executeSingle();
    }

    public static Person getPersonViaPhoneNumber(String phoneNumber) {
        return new Select()
                .from(Person.class)
                .where("phoneNumber = ?", phoneNumber)
                .executeSingle();
    }


    //get all persons
    public static List<Person> getAllPersons() {
        return new Select()
                .from(Person.class)
                .execute();
    }

    public static void savePerson(Person person) {
        if (person != null) {
            person.save();
        }
    }

    public static void deletePerson(Person person) {
        if (person != null && person.getId() != null) {
            person.delete();
        }
    }
}