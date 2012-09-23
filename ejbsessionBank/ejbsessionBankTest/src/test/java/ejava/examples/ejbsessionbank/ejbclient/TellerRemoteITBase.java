package ejava.examples.ejbsessionbank.ejbclient;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;

import ejava.examples.ejbsessionbank.bl.BankException;
import ejava.examples.ejbsessionbank.bo.Account;
import ejava.examples.ejbsessionbank.bo.Owner;
import ejava.examples.ejbsessionbank.ejb.TellerRemote;
import ejava.util.jndi.JNDIUtil;

/**
 * This class contains reusable artifacts and setup/teardown mechanisms for
 * various remote tests in this module.
  */
public class TellerRemoteITBase {
    private static final Log log = LogFactory.getLog(TellerRemoteITBase.class);

    //in-memory jndi.properties environment initialized by derived class
    protected Properties jndiProperties;
    
    //naming technique-specific JNDI name for teller initialized by derived class
    protected String jndiName;
    
    //remote interface to teller, setup in derived class' setUp() method
    protected TellerRemote teller;
    
    //context used to locate remote teller
    private Context jndi;
        
    /**
     * Setup will get the system under test into a common known state prior
     * to running each test. The derived class must have the teller remote
     * reference initialized before this gets invoked. 
     */
    @Before
    public void setUp() throws Exception {
    	log.info("== setUp() ==");
    	
    	assertNotNull("jndiProperties not initialized", jndiProperties);

    	//get the InitialContext from the jndi.properties environment
    	log.debug("getting jndi initial context");
        jndiProperties.list(System.out);
        jndi = new InitialContext(jndiProperties);    
        
       //be sure server is running and application fully deployed
        JNDIUtil.lookup(jndi, TellerRemote.class, jndiName, 15);
    	
        //lookup the remote teller interface
        log.debug("looking up teller remote:" + jndiName);
        teller = (TellerRemote)jndi.lookup(jndiName);
        log.debug("teller=" + teller);
        
        //sanity check who the teller thinks they are talking to
        String user=teller.whoAmI();
        log.debug("caller identity=" + user);
        assertEquals("unexpected user", "anonymous", user);
        
        //get application in a known state for test methods
        cleanup(teller);
    }
    
    @After
    public void tearDown() throws Exception {
    	if (jndi != null) {
    		log.debug("closing JNDI");
    		jndi.close();
    		jndi = null;
    	}
    }
    
    /**
     * Remotes all accounts and owners from the application under test.
     */
    public static void cleanup(TellerRemote teller) throws Exception {
        for (int index=0; ; index+=100) {
        		//do this in pages
            List<Owner> owners = teller.getOwnersLoaded(index, 100);
            if (owners.size() == 0) { break; }
            for (Owner owner : owners) {
                log.debug("removing owner:" + owner);
                for (Account a: owner.getAccounts()) {
                    zeroAccount(teller, a);
                }
                teller.removeOwner(owner.getId());
            }
        }
        
        for (@SuppressWarnings("unused")
		int index=0; ; index+= 100) {
        		//do this in pages
            List<Account> accounts = teller.getAccounts(0, 100);
            if (accounts.size() == 0) { break; }
            for (Account a: accounts) {
                log.debug("cleaning up account:" + a);
                zeroAccount(teller, a);
                teller.closeAccount(a.getAccountNumber());                        
            }
        }
    }
    
    /**
     * Empties a bank account of all funds so that it will be a state
     * that is legal to close.
     * @param teller
     * @param account
     * @throws BankException
     */
    private static void zeroAccount(TellerRemote teller, Account account) throws BankException {
        log.debug("cleaning up account:" + account);
        if (account.getBalance() > 0) {
            account.withdraw(account.getBalance());
           teller.updateAccount(account);
        }
        else if (account.getBalance() < 0) {
            account.deposit(account.getBalance() * -1);
            teller.updateAccount(account);
        }
    }
}
