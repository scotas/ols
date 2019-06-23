package com.scotas.license;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import com.scotas.license.core.ILicense;

public class OlsLicense implements ILicense {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String name;
    private String email;
    private String licenseNumber;
    private Date expiration;
    private String version;

    public OlsLicense() {
        name = "";
        email = "";
        licenseNumber = "";
        Calendar today_plus_year = Calendar.getInstance();
        today_plus_year.add(Calendar.YEAR, 1);
        expiration = today_plus_year.getTime();
        version = "";
    }

    public OlsLicense(String name, String email, Date expiration,
                      String licenseNumber, String version) {
        this.name = name;
        this.email = email;
        this.licenseNumber = licenseNumber;
        this.expiration = expiration;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result =
                prime * result + ((expiration == null) ? 0 : expiration.hashCode());
        result =
                prime * result + ((licenseNumber == null) ? 0 : licenseNumber.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OlsLicense other = (OlsLicense)obj;
        if (email == null) {
            if (other.email != null)
                return false;
        } else if (!email.equals(other.email))
            return false;
        if (expiration == null) {
            if (other.expiration != null)
                return false;
        } else if (!expiration.equals(other.expiration))
            return false;
        if (licenseNumber == null) {
            if (other.licenseNumber != null)
                return false;
        } else if (!licenseNumber.equals(other.licenseNumber))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public boolean validate(Hashtable<?, ?> properties) {
        Date today = (Date)properties.get(OlsLicenseProperties.EXPIRATION);

        return today.before(this.getExpiration());

    }

}
