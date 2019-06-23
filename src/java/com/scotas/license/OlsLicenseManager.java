package com.scotas.license;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scotas.license.core.ILicense;
import com.scotas.license.core.LicenseManager;

/**
 * wrapr to verify every day if the license is valid
 *
 * Generate your keys
 * ==================
 * 		Create the private key (containing informationto create the public key).
 * 		  $ openssl genrsa -out privkey.pem 2048
 * 		  $ openssl pkcs8 -topk8 -in privkey.pem -inform PEM -nocrypt -outform DER -out privkey.der
 * Extract the public key, fur publishing.
 * 	$ openssl rsa -in privkey.pem -out pubkey.der -pubout -outform DER
 *
 *
 * @author jarocena
 *
 */
public class OlsLicenseManager {

    protected static final String CLASS_NAME =
        OlsLicenseManager.class.getName();

    protected static Logger log = LoggerFactory.getLogger(CLASS_NAME);


    private static volatile OlsLicenseManager instance;

    private volatile boolean licenseExpires = true;
    private volatile Date lastVerification = null;
    private LicenseManager licenseCoreManager;
    private DateFormat dateFormat = new SimpleDateFormat("dd-mm-yy");


    private OlsLicenseManager() {
        try {
            URL resource =
                getClass().getResource("/com/scotas/license/pubkey.der");
            InputStream stream = resource.openStream();
            try {
                licenseCoreManager =
                        new com.scotas.license.core.LicenseManager(stream,
                                                                   null);
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            log.error("Error looking for pubkey.der", e);
        }

    }

    public static OlsLicenseManager getInstance() {
        if (instance == null) {
            synchronized (OlsLicenseManager.class) {
                if (instance == null)
                    instance = new OlsLicenseManager();
            }
        }
        return instance;
    }


    public boolean licenseExpires(String licenseFile) {
        Date today = this.getToday();
        if (lastVerification == null || lastVerification.before(today)) {
            synchronized (OlsLicenseManager.class) {
                if (lastVerification == null ||
                    lastVerification.before(today)) {
                    this.lastVerification = today;
                    this.licenseExpires = checkLicense(licenseFile);
                }
            }
        }
        return licenseExpires;
    }

    public ILicense getLicense(String licenseFileName) {
        try {
            URL resource = getClass().getResource(licenseFileName);
            InputStream stream = resource.openStream();
            ILicense license = licenseCoreManager.readLicenseFile(stream);
            return license;

        } catch (InvalidKeyException e) {
            log.error("Invalid Key", e);
            return null;
        } catch (NoSuchAlgorithmException e) {
            // Should never happen
            log.error("License algorithm not found", e);
            return null;
        } catch (SignatureException e) {
            log.error("License signature fail", e);
            return null;
        } catch (IOException e) {
            log.error("Can not read the license", e);
            return null;
        } catch (ClassNotFoundException e) {
            log.error("License class not found", e);
            return null;
        }        
    }
    
    private Date getToday() {
        Date todayTmp = new Date();
        try {
            Date today = dateFormat.parse(dateFormat.format(todayTmp));
            return today;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }

    private boolean checkLicense(String licenseFileName) {

        try {
            URL resource = getClass().getResource(licenseFileName);
            InputStream stream = resource.openStream();
            ILicense license = licenseCoreManager.readLicenseFile(stream);

            Hashtable<OlsLicenseProperties, Object> properties =
                new Hashtable<OlsLicenseProperties, Object>();
            properties.put(OlsLicenseProperties.EXPIRATION, new Date());

            return license.validate(properties);

        } catch (InvalidKeyException e) {
            log.error("Invalid Key", e);
            return false;
        } catch (NoSuchAlgorithmException e) {
            // Should never happen
            log.error("License algorithm not found", e);
            return false;
        } catch (SignatureException e) {
            log.error("License signature fail", e);
            return false;
        } catch (IOException e) {
            log.error("Can not read the license", e);
            return false;
        } catch (ClassNotFoundException e) {
            log.error("License class not found", e);
            return false;
        }
    }

    /**
     ** @param license
     *            the license object.
     * @param file
     *            the location where to save the new license file. If file
     *            exists, it's overwrite.
     * @return successful
     */

    public boolean generateOlsLicence(OlsLicense license, File file) {
        com.scotas.license.core.LicenseManager licenseCoreGenerator;

        try {
            URL privkeyURL =
                getClass().getResource("/com/scotas/license/privkey.der");
            InputStream privkey = privkeyURL.openStream();

            URL pubkeyURL =
                getClass().getResource("/com/scotas/license/pubkey.der");
            InputStream pubkey = pubkeyURL.openStream();

            try {
                licenseCoreGenerator = new LicenseManager(pubkey, privkey);
                licenseCoreGenerator.writeLicense(license, file);
                return true;
            } catch (InvalidKeyException e) {
                log.error("License Generator, invalid privkey.der", e);
                return false;
            } catch (NoSuchAlgorithmException e) {
                log.error("License Generator, algorithm not found", e);
                return false;
            } catch (SignatureException e) {
                log.error("License Generator, singature fails", e);
                return false;
            } catch (IOException e) {
                log.error("License Generator, fails writing license", e);
                return false;
            } finally {
                privkey.close();
                pubkey.close();
            }
        } catch (Exception e) {
            log.error("Error looking for privkey.der", e);
            return false;
        }
    }

}
