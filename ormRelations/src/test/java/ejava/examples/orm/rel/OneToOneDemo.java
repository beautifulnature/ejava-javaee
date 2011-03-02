package ejava.examples.orm.rel;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;

import org.junit.Test;

import ejava.examples.orm.rel.annotated.Applicant;
import ejava.examples.orm.rel.annotated.Borrower;
import ejava.examples.orm.rel.annotated.Person;
import ejava.examples.orm.rel.annotated.Photo;

/**
 * This test case provides a demo of using a class that has been mapped
 * to the database with only basic class annotations. All defaults will
 * be determined by the Java Persistence provider.
 * 
 * @author jcstaff
 * $Id:$
 */
public class OneToOneDemo extends DemoBase {
    private static final String PHOTO_FILE = 
        System.getProperty("photo.file", "images/photo.jpg");
    private byte[] image;
    
    public void setUp() throws Exception {
        super.setUp();
        InputStream is = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(PHOTO_FILE);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (is.read(buffer) != -1) {
            bos.write(buffer);
        }
        image = bos.toByteArray();
        
        //delete any borrowers from other tests so that we don't get PK
        //violoations
        em.createQuery("delete Borrower").executeUpdate();
    }
    
    /**
     * This test provides an example of setting a OneToOne relationship 
     * between two independent obejcts in the database. The Person is 
     * initially created with a FK=null and then its setter is later called
     * to setup the relationship between Person and Photo.
     */
	@Test
    public void testRelateOrphans() {
        log.info("testRelateOrphans");
        
        //create the owning side 
        ejava.examples.orm.rel.annotated.Person person = new Person();
        person.setFirstName("john");
        person.setLastName("doe");
        person.setPhone("410-555-1212");
        
        //create the inverse side 
        ejava.examples.orm.rel.annotated.Photo photo = new Photo();        
        photo.setImage(image);        
        
        //create the person and photo detached
        assertTrue(person.getId() == 0);
        assertTrue(photo.getId() == 0);
        em.persist(person);        
        em.persist(photo);
        assertTrue(person.getId() != 0);
        assertTrue(photo.getId() != 0);
        log.info("created person:" + person);
        log.info("     and photo:" + photo);

        //add photo to person
        person.setPhoto(photo); //this sets the FK in person
        log.info("added photo to person:" + person);
        
        //verify what we can get from DB
        em.flush();
        em.clear();
        Person person2 = em.find(Person.class, person.getId());
        assertNotNull(person2);
        assertNotNull(person2.getPhoto());
        log.info("found person:" + person2);
    }        

    /**
     * This test provides an example of persisting an object tree from the
     * owning side. The tree is very small, consisting of only the Person
     * and Photo, but demonstrates the Cascade.PERSIST capability.
     */
	@Test
    public void testCacadePersist() {
        log.info("testCascadePersist");
        
        //create the owning side 
        ejava.examples.orm.rel.annotated.Person person = new Person();
        person.setFirstName("jane");
        person.setLastName("doe");
        person.setPhone("410-555-1212");
        
        //create the inverse side 
        ejava.examples.orm.rel.annotated.Photo photo = new Photo();        
        photo.setImage(image);   
        
        //add photo to person before persist
        person.setPhoto(photo);
        
        //create the person and cascade to photo
        assertTrue(person.getId() == 0);
        assertTrue(photo.getId() == 0);
        assertNotNull(person.getPhoto());
        em.persist(person);        
        assertTrue(person.getId() != 0);
        assertTrue(photo.getId() != 0);
        log.info("created person with photo:" + person);

        //verify what we can get from DB
        em.flush();
        em.clear();
        Person person2 = em.find(Person.class, person.getId());
        assertNotNull(person2);
        assertNotNull(person2.getPhoto());
        log.info("found person:" + person2);
    }        

    /**
     * This test demonstrates an issue I found with inserting objects on the 
     * inverse side of a relationship during the owner's setter(). If the 
     * object uses generated values, the id within the Photo object is not
     * being updated. Once can't merge or refresh the photo until a separate
     * set of objects are created using the find. 
     *
     */
	@Test
    public void testPersistOnSetter() {
        log.info("testSetPersistedWithNew");
        
        //create the owning side 
        ejava.examples.orm.rel.annotated.Person person = new Person();
        person.setFirstName("julie");
        person.setLastName("doe");
        person.setPhone("410-555-1212");
        
        //create the person person without the photo
        assertTrue(person.getId() == 0);
        em.persist(person);        
        assertTrue(person.getId() != 0);
        log.info("created person without photo:" + person);        
        
        //create the inverse side 
        ejava.examples.orm.rel.annotated.Photo photo = new Photo();        
        photo.setImage(image);   
        
        //add photo to the managed person
        assertTrue(photo.getId() == 0);        
        person.setPhoto(photo);
        log.info("added photo to person:" + person);
        em.flush();  //flush to make sure photo object gets PK from DB
        assertTrue("hey! primary key wasn't being set during setter", 
                   photo.getId() != 0); //note that although the photo is in 
                                       //the database, our photo object was
                                       //not updated with its PK        

        //verify what we can get from DB
        em.flush();
        em.clear();
        Person person2 = em.find(Person.class, person.getId());
        assertNotNull(person2);
        assertNotNull(person2.getPhoto());
        log.info("found person:" + person2);
    }        
    
    /**
     * This demonstrates a OneToOne uni-directional relationship between 
     * the Borrower and Person where they are being joined by primary key
     * instead of a separate column in the borrower table. 
     *
     */
	@Test
    public void testPrimaryKeyJoin() {
        log.info("testPrimaryKeyJoin");
        
        //create the person we'll use in the relationship
        ejava.examples.orm.rel.annotated.Person person = new Person();
        person.setFirstName("jerome");
        person.setLastName("doe");
        person.setPhone("410-555-1212");
        em.persist(person);
        log.info("created person:" + person);
        
        //create the Borrower, who requires a Person for its identity
        ejava.examples.orm.rel.annotated.Borrower borrower = 
            new Borrower(person);
        borrower.setStartDate(new Date());
        assertEquals(person.getId(), borrower.getId()); //ctor copies PK         
        
        //persist the borrower, creating the relationship to person
        em.persist(borrower);
        log.info("created borrower:" + borrower);
        
        //lets add a photo to make the finder more interesting
        person.setPhoto(new Photo(image));
        log.info("added photo to borrower's person:" + borrower);

        //verify what we can get from DB
        em.flush();
        em.clear();
        Borrower borrower2 = em.find(Borrower.class, borrower.getId());
        assertNotNull(borrower2);
        log.info("found person:" + borrower2);
        assertTrue("jerome doe".equals(borrower2.getName()));
    }
    
    /**
     * This demonstrates a OneToOne bi-directional relationship between
     * two logically independent object; Applicant and Borrower. Ignore the
     * fact they they both have a relationship to a Person. Focus on the fact
     * that they can both independently exist and that the Applicant physically
     * owns the relationship by hosting the foreign key field.
     */
	@Test
    public void testOneToOneBiDirectional() {
        log.info("testOneToOneBiDirectional");
        
        //create our person (the person must exist)
        ejava.examples.orm.rel.annotated.Person person = new Person();
        person.setFirstName("james");
        person.setLastName("doe");
        person.setPhone("410-555-1212");
        em.persist(person);
        log.info("created person:" + person);
                
        //instantiate our two objects
        ejava.examples.orm.rel.annotated.Applicant applicant = new Applicant();
        applicant.setIdentity(person);
        log.info("instantiated applicant:" + applicant);
        
        ejava.examples.orm.rel.annotated.Borrower borrower = 
            new Borrower(person);
        borrower.setStartDate(new Date());
        log.info("instantiated borrower:" + borrower);
        
        //place in DB
        em.persist(applicant);
        em.persist(borrower);
        log.info("created applicant in DB:" + applicant);
        log.info("    and borrower:" + borrower);
        em.flush();
        em.clear();
        
        //locate them from DB
        Applicant applicant2 = em.find(Applicant.class, applicant.getId());
        Borrower borrower2 = em.find(Borrower.class, borrower.getId());
        assertNotNull(applicant2);
        assertNotNull(borrower2);
        log.info("found unrelated applicant in DB:" + applicant2);
        log.info("    and borrower:" + borrower2);
        
        //form relationship
        borrower2.setApplication(applicant2); //set inverse side
        applicant2.setBorrower(borrower2);    //set owning side
        log.info("related applicant in DB:" + applicant2);
        log.info("    and borrower:" + borrower2);
        em.flush();
        em.clear();
        
        //locate them from DB
        Applicant applicant3 = em.find(Applicant.class, applicant.getId());
        Borrower borrower3 = em.find(Borrower.class, borrower.getId());
        assertNotNull(applicant3);
        assertNotNull(borrower3);
        log.info("found related applicant in DB:" + applicant3);
        log.info("    and borrower:" + borrower3);
        assertEquals(applicant.getId(), borrower3.getApplication().getId());
        assertEquals(borrower.getId(), applicant3.getBorrower().getId());
        
        //remove borrower from DB
        applicant3.setBorrower(null);
        em.remove(borrower3);
        log.info("removed borrower from applicant and DB");        

        //verify borrower not associated with applicant
        em.flush();
        em.clear();
        Applicant applicant4 = em.find(Applicant.class, applicant.getId());
        Borrower borrower4 = em.find(Borrower.class, borrower.getId());
        assertNotNull(applicant4);
        assertNull(borrower4);
        log.info("found related applicant in DB:" + applicant4);
        log.info("    but didn't find borrower:" + borrower4);
        assertNull(applicant4.getBorrower());
    }
}
