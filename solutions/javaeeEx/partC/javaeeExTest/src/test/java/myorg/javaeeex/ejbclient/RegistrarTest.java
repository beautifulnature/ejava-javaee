package myorg.javaeeex.ejbclient;

import javax.naming.InitialContext;

import junit.framework.TestCase;

import myorg.javaeeex.ejb.RegistrarRemote;
import myorg.javaeeex.bo.Person;
import myorg.javaeeex.bo.Address;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RegistrarTest extends TestCase {
    Log log = LogFactory.getLog(RegistrarTest.class);
    InitialContext jndi;
    String registrarJNDI = System.getProperty("jndi.name.registrar");
    RegistrarRemote registrar;
    
    public void setUp() throws Exception {
        assertNotNull("jndi.name.registrar not supplied", registrarJNDI);

        log.debug("getting jndi initial context");
        jndi = new InitialContext();    
        log.debug("jndi=" + jndi.getEnvironment());
        jndi.lookup("/"); //do a quick comms check of JNDI
        
        log.debug("jndi name:" + registrarJNDI);
        registrar = (RegistrarRemote)jndi.lookup(registrarJNDI);
    }

    public void testPing() {
        log.info("*** testPing ***");
        registrar.ping();
    }
    
    protected Person makePerson() {
        Person person = new Person();
        person.setFirstName("joe");
        person.setLastName("smith");
        person.setSsn("123");
        Address address = new Address(0,"street1","city1", "state1", "zip1");
        person.getAddresses().add(address);
        return person;
    }

    public void testCreatePerson() throws Exception {
        log.info("*** testCreatePerson ***");
        
        Person person = makePerson();
        Person person2 = registrar.createPerson(person);
            //note that our original Person does not have an ID
        assertEquals("unexpected id", 0, person.getId());
            //it is separate from the one returned
        assertFalse("unexpected id:" + person2.getId(), person2.getId()==0);
        
        Person person3 = registrar.getPersonById(person2.getId());
        assertEquals("unexpected name",
            person.getFirstName(), 
            person3.getFirstName());
    }
}