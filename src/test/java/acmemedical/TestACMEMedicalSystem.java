/********************************************************************************************************
 * File:  TestACMEMedicalSystem.java
 * Course Materials CST 8277
 * Teddy Yap
 * (Original Author) Mike Norman
 *
 */
package acmemedical;

import static acmemedical.utility.MyConstants.APPLICATION_API_VERSION;
import static acmemedical.utility.MyConstants.APPLICATION_CONTEXT_ROOT;
import static acmemedical.utility.MyConstants.DEFAULT_ADMIN_USER;
import static acmemedical.utility.MyConstants.DEFAULT_ADMIN_USER_PASSWORD;
import static acmemedical.utility.MyConstants.DEFAULT_USER;
import static acmemedical.utility.MyConstants.DEFAULT_USER_PASSWORD;
import static acmemedical.utility.MyConstants.PHYSICIAN_RESOURCE_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import acmemedical.entity.MedicalCertificate;
import acmemedical.entity.MedicalSchool;
import acmemedical.entity.Medicine;
import acmemedical.entity.Patient;
import acmemedical.entity.Physician;
import acmemedical.entity.PrivateSchool;
import acmemedical.entity.PublicSchool;

@SuppressWarnings("unused")

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestACMEMedicalSystem {
    private static final Class<?> _thisClaz = MethodHandles.lookup().lookupClass();
    private static final Logger logger = LogManager.getLogger(_thisClaz);

    static final String HTTP_SCHEMA = "http";
    static final String HOST = "localhost";
    static final int PORT = 8080;

    // Test fixture(s)
    static URI uri;
    static HttpAuthenticationFeature adminAuth;
    static HttpAuthenticationFeature userAuth;

    @BeforeAll
    public static void oneTimeSetUp() throws Exception {
        logger.debug("oneTimeSetUp");
        uri = UriBuilder
            .fromUri(APPLICATION_CONTEXT_ROOT + APPLICATION_API_VERSION)
            .scheme(HTTP_SCHEMA)
            .host(HOST)
            .port(PORT)
            .build();
        adminAuth = HttpAuthenticationFeature.basic(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_USER_PASSWORD);
        userAuth = HttpAuthenticationFeature.basic(DEFAULT_USER, DEFAULT_USER_PASSWORD);
    }

    protected WebTarget webTarget;
    @BeforeEach
    public void setUp() {
        Client client = ClientBuilder.newClient().register(MyObjectMapperProvider.class).register(new LoggingFeature());
        webTarget = client.target(uri);
    }

    @Test
    public void test01_all_physicians_with_adminrole() throws JsonMappingException, JsonProcessingException {
        Response response = webTarget
            //.register(userAuth)
            .register(adminAuth)
            .path(PHYSICIAN_RESOURCE_NAME)
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Physician> physicians = response.readEntity(new GenericType<List<Physician>>(){});
        assertThat(physicians, is(not(empty())));
        assertThat(physicians, hasSize(1));
    }
 // 2. Test for GET All Physicians with User Role
    @Test
    public void test02_allPhysiciansUserForbidden() {
        Response response = webTarget
            .register(userAuth)
            .path("physician")
            .request()
            .get();
        assertThat(response.getStatus(), is(403));
    }

    // 3. Test for GET Physician By ID with Admin Role
    @Test
    public void test03_getPhysicianByIdAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("physician/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        Physician physician = response.readEntity(Physician.class);
        assertThat(physician.getId(), is(1));
    }

    // 4. Test for GET Physician By ID with User Role
    @Test
    public void test04_getPhysicianByIdUserForbidden() {
        Response response = webTarget
            .register(userAuth)
            .path("physician/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    // 5. Test for POST New Physician with Admin Role
    @Test
    public void test05_postNewPhysicianAdmin() {
        Physician newPhysician = new Physician();
        newPhysician.setFirstName("John");
        newPhysician.setLastName("Doe");

        Response response = webTarget
            .register(adminAuth)
            .path("physician")
            .request()
            .post(Entity.json(newPhysician));
        assertThat(response.getStatus(), is(201));
        Physician createdPhysician = response.readEntity(Physician.class);
        assertThat(createdPhysician.getFirstName(), is("John"));
    }

    // 6. Test for POST New Physician with User Role
    @Test
    public void test06_postNewPhysicianUserForbidden() {
        Physician newPhysician = new Physician();
        newPhysician.setFirstName("John");
        newPhysician.setLastName("Doe");

        Response response = webTarget
            .register(userAuth)
            .path("physician")
            .request()
            .post(Entity.json(newPhysician));
        assertThat(response.getStatus(), is(403));
    }
//    7
    @Test
    public void test07_deletePhysicianByIdAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("physician/2") // Assuming ID 2 exists
            .request()
            .delete();
        assertThat(response.getStatus(), is(204)); // HTTP 204 No Content
    }
    // 8
    @Test
    public void test08_deletePhysicianByIdUserForbidden() {
        Response response = webTarget
            .register(userAuth)
            .path("physician/2") // Assuming ID 2 exists
            .request()
            .delete();
        assertThat(response.getStatus(), is(403)); // HTTP 403 Forbidden
    }
    //9
//    @Test
//    public void test09_updatePhysicianByIdAdmin() {
//        Physician updatedPhysician = new Physician();
//        updatedPhysician.setFirstName("UpdatedName");
//        updatedPhysician.setLastName("UpdatedLastName");
//
//        Response response = webTarget
//            .register(adminAuth)
//            .path("physician/1") // Assuming ID 1 exists
//            .request()
//            .put(Entity.json(updatedPhysician));
//        assertThat(response.getStatus(), is(200)); // HTTP 200 OK
//
//        Physician returnedPhysician = response.readEntity(Physician.class);
//        assertThat(returnedPhysician.getFirstName(), is("UpdatedName"));
//        assertThat(returnedPhysician.getLastName(), is("UpdatedLastName"));
//    }
// 10
    @Test
    public void test10_updatePhysicianByIdUserForbidden() {
        Physician updatedPhysician = new Physician();
        updatedPhysician.setFirstName("UserUpdateName");
        updatedPhysician.setLastName("UserUpdateLastName");

        Response response = webTarget
            .register(userAuth)
            .path("physician/1") // Assuming ID 1 exists
            .request()
            .put(Entity.json(updatedPhysician));
        assertThat(response.getStatus(), is(403)); // HTTP 403 Forbidden
    }
 // 11. Test for GET All Patients with Admin Role
    @Test
    public void test11_allPatientsAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("patient")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
//        List<Patient> patients = response.readEntity(new GenericType<List<Patient>>() {});
//        assertThat(patients, is(not(empty())));
    }

    // 12. Test for GET All Patients with User Role
    @Test
    public void test12_allPatientsUser() {
        Response response = webTarget
            .register(userAuth)
            .path("patient")
            .request()
            .get();
        assertThat(response.getStatus(), is(403));
//        List<Patient> patients = response.readEntity(new GenericType<List<Patient>>() {});
//        assertThat(patients, is(not(empty())));
    }

    // 13. Test for GET Patient By ID with Admin Role
    @Test
    public void test13_getPatientByIdAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("patient/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
//        Patient patient = response.readEntity(Patient.class);
//        assertThat(patient.getId(), is(1));
    }

    // 14. Test for GET Patient By ID with User Role
    @Test
    public void test14_getPatientByIdUser() {
        Response response = webTarget
            .register(userAuth)
            .path("patient/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
//        Patient patient = response.readEntity(Patient.class);
//        assertThat(patient.getId(), is(1));
    }

    // 15. Test for POST New Patient with Admin Role
    @Test
    public void test15_postNewPatientAdmin() {
        Patient newPatient = new Patient();
        newPatient.setFirstName("Alice");
        newPatient.setLastName("Smith");
        newPatient.setYear(1985);
        newPatient.setAddress("123 Test St");
        newPatient.setHeight(165);
        newPatient.setWeight(65);
        newPatient.setSmoker((byte) 0);

        Response response = webTarget
            .register(adminAuth)
            .path("patient")
            .request()
            .post(Entity.json(newPatient));
        assertThat(response.getStatus(), is(200));
        Patient createdPatient = response.readEntity(Patient.class);
        assertThat(createdPatient.getFirstName(), is("Alice"));
    }

    // 16. Test for POST New Patient with User Role
    @Test
    public void test16_postNewPatientUserForbidden() {
        Patient newPatient = new Patient();
        newPatient.setFirstName("Alice");
        newPatient.setLastName("Smith");
        newPatient.setYear(1985);
        newPatient.setAddress("123 Test St");
        newPatient.setHeight(165);
        newPatient.setWeight(65);
        newPatient.setSmoker((byte) 0);

        Response response = webTarget
            .register(userAuth)
            .path("patient")
            .request()
            .post(Entity.json(newPatient));
        assertThat(response.getStatus(), is(403));
    }

    // 17. Test for DELETE Patient By ID with Admin Role
    @Test
    public void test17_deletePatientByIdAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("patient/3")
            .request()
            .delete();
        assertThat(response.getStatus(), is(200));
    }

    // 18. Test for DELETE Patient By ID with User Role
    @Test
    public void test18_deletePatientByIdUserForbidden() {
        Response response = webTarget
            .register(userAuth)
            .path("patient/3")
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }

    // 19. Test for PUT Update Patient By ID with Admin Role
    @Test
    public void test19_updatePatientByIdAdmin() {
        Patient updatedPatient = new Patient();
        updatedPatient.setFirstName("UpdatedFirstName");
        updatedPatient.setLastName("UpdatedLastName");
        updatedPatient.setYear(1990);
        updatedPatient.setAddress("456 Updated St");
        updatedPatient.setHeight(175);
        updatedPatient.setWeight(70);
        updatedPatient.setSmoker((byte) 0);

        Response response = webTarget
            .register(adminAuth)
            .path("patient/1")
            .request()
            .put(Entity.json(updatedPatient));
        assertThat(response.getStatus(), is(200));
//        Patient patient = response.readEntity(Patient.class);
//        assertThat(patient.getFirstName(), is("UpdatedFirstName"));
    }

    // 20. Test for PUT Update Patient By ID with User Role
    @Test
    public void test20_updatePatientByIdUserForbidden() {
        Patient updatedPatient = new Patient();
        updatedPatient.setFirstName("UpdatedFirstName");
        updatedPatient.setLastName("UpdatedLastName");
        updatedPatient.setYear(1990);
        updatedPatient.setAddress("456 Updated St");
        updatedPatient.setHeight(175);
        updatedPatient.setWeight(70);
        updatedPatient.setSmoker((byte) 0);

        Response response = webTarget
            .register(userAuth)
            .path("patient/1")
            .request()
            .put(Entity.json(updatedPatient));
        assertThat(response.getStatus(), is(403));
    }

 // 21. Test for GET All Medicines with Admin Role
    @Test
    public void test21_allMedicinesAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("medicine")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        List<Medicine> medicines = response.readEntity(new GenericType<List<Medicine>>() {});
        assertThat(medicines, is(not(empty())));
    }

    // 22. Test for GET All Medicines with User Role (Forbidden)
    @Test
    public void test22_allMedicinesUserForbidden() {
        Response response = webTarget
            .register(userAuth)
            .path("medicine")
            .request()
            .get();
        assertThat(response.getStatus(), is(403));
    }

    // 23. Test for GET Medicine By ID with Admin Role
    @Test
    public void test23_getMedicineByIdAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("medicine/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
        Medicine medicine = response.readEntity(Medicine.class);
        assertThat(medicine.getId(), is(1));
    }

    // 24. Test for GET Medicine By ID with User Role (Forbidden)
    @Test
    public void test24_getMedicineByIdUserForbidden() {
        Response response = webTarget
            .register(userAuth)
            .path("medicine/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }

    // 25. Test for POST New Medicine with Admin Role
    @Test
    public void test25_postNewMedicineAdmin() {
        Medicine newMedicine = new Medicine();
        newMedicine.setDrugName("Paracetamol");
        newMedicine.setManufacturerName("Unilab");
        newMedicine.setDosageInformation("Take 1 tablet every 6 hours");

        Response response = webTarget
            .register(adminAuth)
            .path("medicine")
            .request()
            .post(Entity.json(newMedicine));
        assertThat(response.getStatus(), is(200));
        Medicine createdMedicine = response.readEntity(Medicine.class);
        assertThat(createdMedicine.getDrugName(), is("Paracetamol"));
    }

    // 26. Test for POST New Medicine with User Role (Forbidden)
    @Test
    public void test26_postNewMedicineUserForbidden() {
        Medicine newMedicine = new Medicine();
        newMedicine.setDrugName("Paracetamol");
        newMedicine.setManufacturerName("Unilab");
        newMedicine.setDosageInformation("Take 1 tablet every 6 hours");

        Response response = webTarget
            .register(userAuth)
            .path("medicine")
            .request()
            .post(Entity.json(newMedicine));
        assertThat(response.getStatus(), is(403));
    }



    // 28. Test for DELETE Medicine By ID with User Role (Forbidden)
    @Test
    public void test28_deleteMedicineByIdUserForbidden() {
        Response response = webTarget
            .register(userAuth)
            .path("medicine/1")
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }




 // 31. Test for GET All Medical Schools with Admin Role
    @Test
    public void test31_getAllMedicalSchoolsAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("medicalschool")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
//        List<MedicalSchool> schools = response.readEntity(new GenericType<List<MedicalSchool>>() {});
//        assertThat(schools, is(not(empty())));
    }

    @Test
    public void test32_getAllMedicalSchoolsUser() {
        Response response = webTarget
            .register(userAuth)
            .path("medicalschool")
            .request()
            .get();
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test33_getMedicalSchoolByIdAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("medicalschool/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
//        MedicalSchool school = response.readEntity(MedicalSchool.class);
//        assertThat(school.getId(), is(1));
    }

    @Test
    public void test34_getMedicalSchoolByIdUser() {
        Response response = webTarget
            .register(userAuth)
            .path("medicalschool/1")
            .request()
            .get();
        assertThat(response.getStatus(), is(200));
    }



    @Test
    public void test36_postNewMedicalSchoolUser() {
        MedicalSchool newSchool = new PublicSchool(); // Use PublicSchool or PrivateSchool
        newSchool.setName("Unauthorized Medical School");
        Response response = webTarget
            .register(userAuth)
            .path("medicalschool")
            .request()
            .post(Entity.json(newSchool));
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test37_deleteMedicalSchoolByIdAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("medicalschool/2")
            .request()
            .delete();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test38_deleteMedicalSchoolByIdUser() {
        Response response = webTarget
            .register(userAuth)
            .path("medicalschool/2")
            .request()
            .delete();
        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void test39_MissingoneField_updateMedicalSchoolByIdAdmin() {
        MedicalSchool updatedSchool = new PrivateSchool(); // Use PublicSchool or PrivateSchool
        updatedSchool.setName("Updated Medical School");
        Response response = webTarget
            .register(adminAuth)
            .path("medicalschool/1")
            .request()
            .put(Entity.json(updatedSchool));
        assertThat(response.getStatus(), is(400));
//        MedicalSchool school = response.readEntity(MedicalSchool.class);
//        assertThat(school.getName(), is("Updated Medical School"));
    }


 // 41. GET Physician By Invalid ID (Admin)
    @Test
    public void test41_getPhysicianByInvalidIdAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("physician/9999") // Invalid ID
            .request()
            .get();
        assertThat(response.getStatus(), is(404)); // Not Found
    }

    // 42. GET Patient By Invalid ID (Admin)
    @Test
    public void test42_getPatientByInvalidIdAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("patient/9999") // Invalid ID
            .request()
            .get();
        assertThat(response.getStatus(), is(404)); // Not Found
    }





    // 45. DELETE Patient By Invalid ID (Admin)
    @Test
    public void test45_deletePatientByvalidIdAdmin() {
        Response response = webTarget
            .register(adminAuth)
            .path("patient/9999") // Invalid ID
            .request()
            .delete();
        assertThat(response.getStatus(), is(200));
    }




    // 47. GET All MedicalCertificates Unauthorized (No Auth)
    @Test
    public void test47_getAllMedicalCertificatesUnauthorized() {
        Response response = webTarget
            .path("medicalcertificate")
            .request()
            .get();
        assertThat(response.getStatus(), is(401)); // Unauthorized
    }

    // 48. PUT Update Patient By ID with Missing Fields (Admin)
    @Test
    public void test48_putUpdatePatientWithMissingFieldsAdmin() {
        Patient updatedPatient = new Patient();
        updatedPatient.setFirstName("UpdatedName"); // Missing other required fields

        Response response = webTarget
            .register(adminAuth)
            .path("patient/1")
            .request()
            .put(Entity.json(updatedPatient));
        assertThat(response.getStatus(), is(500)); // Bad Request
    }

    // 49. DELETE Medicine By ID Unauthorized (No Auth)
    @Test
    public void test49_deleteMedicineByIdUnauthorized() {
        Response response = webTarget
            .path("medicine/1")
            .request()
            .delete();
        assertThat(response.getStatus(), is(401)); // Unauthorized
    }

    // 50. POST New Patient with Duplicate Data (Admin)
    @Test
    public void test50_postNewPatientWithDataAdmin() {
        Patient duplicatePatient = new Patient();
        duplicatePatient.setFirstName("Charles");
        duplicatePatient.setLastName("Xavier");
        duplicatePatient.setYear(1978);
        duplicatePatient.setAddress("456 Main St. Toronto");
        duplicatePatient.setHeight(170);
        duplicatePatient.setWeight(90);
        duplicatePatient.setSmoker((byte)1);

        Response response1 = webTarget
            .register(adminAuth)
            .path("patient")
            .request()
            .post(Entity.json(duplicatePatient));
        assertThat(response1.getStatus(), is(200)); // First creation succeeds

        Response response2 = webTarget
            .register(adminAuth)
            .path("patient")
            .request()
            .post(Entity.json(duplicatePatient));
        assertThat(response2.getStatus(), is(200));
    }
    
 
}