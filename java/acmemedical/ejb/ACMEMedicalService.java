package acmemedical.ejb;

import static acmemedical.utility.MyConstants.*;

import java.io.Serializable;
import java.util.*;

import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.transaction.Transactional;
import static acmemedical.entity.MedicalSchool.SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME;
import static acmemedical.entity.MedicalSchool.IS_DUPLICATE_QUERY_NAME;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import acmemedical.entity.*;

/**
 * Stateless Singleton EJB Bean - ACMEMedicalService
 */
@Singleton
public class ACMEMedicalService implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LogManager.getLogger();

    @PersistenceContext(name = PU_NAME)
    protected EntityManager em;

    @Inject
    protected jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash pbAndjPasswordHash;

    // ************************************
    // PHYSICIAN MANAGEMENT
    // ************************************

    public List<Physician> getAllPhysicians() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Physician> cq = cb.createQuery(Physician.class);
        cq.select(cq.from(Physician.class));
        return em.createQuery(cq).getResultList();
    }

    public Physician getPhysicianById(int id) {
        return em.find(Physician.class, id);
    }

    @Transactional
    public Physician persistPhysician(Physician newPhysician) {
        em.persist(newPhysician);
        return newPhysician;
    }

    @Transactional
    public void buildUserForNewPhysician(Physician newPhysician) {
        SecurityUser userForNewPhysician = new SecurityUser();
        userForNewPhysician.setUsername(
            DEFAULT_USER_PREFIX + "_" + newPhysician.getFirstName() + "." + newPhysician.getLastName());
        Map<String, String> pbAndjProperties = new HashMap<>();
        pbAndjProperties.put(PROPERTY_ALGORITHM, DEFAULT_PROPERTY_ALGORITHM);
        pbAndjProperties.put(PROPERTY_ITERATIONS, DEFAULT_PROPERTY_ITERATIONS);
        pbAndjProperties.put(PROPERTY_SALT_SIZE, DEFAULT_SALT_SIZE);
        pbAndjProperties.put(PROPERTY_KEY_SIZE, DEFAULT_KEY_SIZE);
        pbAndjPasswordHash.initialize(pbAndjProperties);
        String pwHash = pbAndjPasswordHash.generate(DEFAULT_USER_PASSWORD.toCharArray());
        userForNewPhysician.setPwHash(pwHash);
        userForNewPhysician.setPhysician(newPhysician);

        // Completed TODO ACMECS01 - Use NamedQuery on SecurityRole to find USER_ROLE
        SecurityRole userRole = em.createNamedQuery(SecurityRole.FIND_USER_ROLE, SecurityRole.class)
                                  .setParameter(PARAM1, USER_ROLE)
                                  .getSingleResult();

        userForNewPhysician.getRoles().add(userRole);
        userRole.getUsers().add(userForNewPhysician);
        em.persist(userForNewPhysician);
    }

//    @Transactional
//    public void deletePhysicianById(int id) {
//        Physician physician = getPhysicianById(id);
//        if (physician != null) {
//            em.refresh(physician);
//
//            
//            TypedQuery<SecurityUser> findUser = em.createNamedQuery(SecurityUser.USER_FOR_OWNING_PHYSICIAN_QUERY, SecurityUser.class)
//                                                  .setParameter(PARAM1, physician.getId());
//            SecurityUser sUser = findUser.getSingleResult();
//
//            if (sUser != null) {
//                em.remove(sUser);
//            }
//            em.remove(physician);
//        }
//    }
    @Transactional
    public void deletePhysicianById(int id) {
        Physician physician = getPhysicianById(id);
        if (physician != null) {
            em.refresh(physician);

         // Completed TODO ACMECS02 - Use NamedQuery to find and delete related SecurityUser
            TypedQuery<SecurityUser> findUser = em.createNamedQuery(SecurityUser.USER_FOR_OWNING_PHYSICIAN_QUERY, SecurityUser.class)
                                                  .setParameter(PARAM1, physician.getId());

            SecurityUser sUser = null;
            try {
                sUser = findUser.getSingleResult();
            } catch (NoResultException e) {
                LOG.warn("No SecurityUser found for Physician with ID: {}", id);
            }

            // If a SecurityUser is found, remove it
            if (sUser != null) {
                em.remove(sUser);
            }

            // Remove the Physician
            em.remove(physician);
        } else {
            throw new EntityNotFoundException("Physician with ID " + id + " does not exist.");
        }
    }


    @Transactional
    public Medicine setMedicineForPhysicianPatient(int physicianId, int patientId, Medicine newMedicine) {
        Physician physicianToBeUpdated = em.find(Physician.class, physicianId);
        if (physicianToBeUpdated != null) { // Physician exists
            Set<Prescription> prescriptions = physicianToBeUpdated.getPrescriptions();
            prescriptions.forEach(p -> {
                if (p.getPatient().getId() == patientId) {
                    if (p.getMedicine() != null) { // Medicine exists
                        Medicine medicine = em.find(Medicine.class, p.getMedicine().getId());
                        medicine.setMedicine(newMedicine.getDrugName(),
                                             newMedicine.getManufacturerName(),
                                             newMedicine.getDosageInformation());
                        em.merge(medicine);
                    } else { // Medicine does not exist
                        p.setMedicine(newMedicine);
                        em.merge(physicianToBeUpdated);
                    }
                }
            });
            return newMedicine;
        }
        return null; // Physician doesn't exist
    }

    @Transactional
    public Physician updatePhysicianById(int id, Physician physicianWithUpdates) {
        Physician physicianToBeUpdated = getPhysicianById(id);
        if (physicianToBeUpdated != null) {
            em.refresh(physicianToBeUpdated);
            em.merge(physicianWithUpdates);
            em.flush();
        }
        return physicianToBeUpdated;
    }

    // ************************************
    // MEDICAL SCHOOL MANAGEMENT
    // ************************************

    public List<MedicalSchool> getAllMedicalSchools() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MedicalSchool> cq = cb.createQuery(MedicalSchool.class);
        cq.select(cq.from(MedicalSchool.class));
        return em.createQuery(cq).getResultList();
    }

    public MedicalSchool getMedicalSchoolById(int id) {
        TypedQuery<MedicalSchool> specificMedicalSchoolQuery = em.createNamedQuery(SPECIFIC_MEDICAL_SCHOOL_QUERY_NAME, MedicalSchool.class);
        specificMedicalSchoolQuery.setParameter(PARAM1, id);
        return specificMedicalSchoolQuery.getSingleResult();
    }

    @Transactional
    public MedicalSchool persistMedicalSchool(MedicalSchool newMedicalSchool) {
        em.persist(newMedicalSchool);
        return newMedicalSchool;
    }

    @Transactional
    public MedicalSchool updateMedicalSchool(int id, MedicalSchool updatingMedicalSchool) {
        MedicalSchool medicalSchoolToBeUpdated = getMedicalSchoolById(id);
        if (medicalSchoolToBeUpdated != null) {
            em.refresh(medicalSchoolToBeUpdated);
            medicalSchoolToBeUpdated.setName(updatingMedicalSchool.getName());
            em.merge(medicalSchoolToBeUpdated);
            em.flush();
        }
        return medicalSchoolToBeUpdated;
    }

    @Transactional
    public MedicalSchool deleteMedicalSchool(int id) {
        MedicalSchool ms = getMedicalSchoolById(id);
        if (ms != null) {
            Set<MedicalTraining> medicalTrainings = ms.getMedicalTrainings();
            List<MedicalTraining> list = new LinkedList<>();
            medicalTrainings.forEach(list::add);
            list.forEach(mt -> {
                if (mt.getCertificate() != null) {
                    MedicalCertificate mc = getById(MedicalCertificate.class, MedicalCertificate.ID_CERTIFICATE_QUERY_NAME, mt.getCertificate().getId());
                    mc.setMedicalTraining(null);
                }
                mt.setCertificate(null);
                em.merge(mt);
            });
            em.remove(ms);
            return ms;
        }
        return null;
    }

    public boolean isDuplicated(MedicalSchool newMedicalSchool) {
        TypedQuery<Long> allMedicalSchoolsQuery = em.createNamedQuery(IS_DUPLICATE_QUERY_NAME, Long.class);
        allMedicalSchoolsQuery.setParameter(PARAM1, newMedicalSchool.getName());
        return (allMedicalSchoolsQuery.getSingleResult() >= 1);
    }

    // ************************************
    // MEDICAL TRAINING MANAGEMENT
    // ************************************

//    public List<MedicalTraining> getAllMedicalTraining() {
//        CriteriaBuilder cb = em.getCriteriaBuilder();
//        CriteriaQuery<MedicalTraining> cq = cb.createQuery(MedicalTraining.class);
//        cq.select(cq.from(MedicalTraining.class));
//        return em.createQuery(cq).getResultList();
//    }
//
//    @Transactional
//    public MedicalTraining persistMedicalTraining(MedicalTraining newMedicalTraining) {
//        em.persist(newMedicalTraining);
//        return newMedicalTraining;
//    }
//
//    @Transactional
//    public void deleteMedicalTrainingById(int id) {
//        MedicalTraining newMedicalTraining = getById(MedicalTraining.class, MedicalTraining.FIND_BY_ID, id);
//        if (newMedicalTraining != null) {
//            em.remove(newMedicalTraining);
//        }
//    }
//
//    public MedicalTraining getMedicalTrainingById(int id) {
//        TypedQuery<MedicalTraining> query = em.createNamedQuery(MedicalTraining.FIND_BY_ID, MedicalTraining.class);
//        query.setParameter(PARAM1, id);
//        return query.getSingleResult();
//    }
    public List<MedicalTraining> getAllMedicalTraining() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MedicalTraining> cq = cb.createQuery(MedicalTraining.class);
        cq.select(cq.from(MedicalTraining.class));
        return em.createQuery(cq).getResultList();
    }

    @Transactional
    public MedicalTraining persistMedicalTraining(MedicalTraining newMedicalTraining) {
        if (newMedicalTraining.getMedicalSchool() == null) {
            throw new IllegalArgumentException("MedicalSchool cannot be null.");
        }

        MedicalSchool school = em.find(MedicalSchool.class, newMedicalTraining.getMedicalSchool().getId());
        if (school == null) {
            throw new IllegalArgumentException("MedicalSchool with ID " + newMedicalTraining.getMedicalSchool().getId() + " does not exist.");
        }
        newMedicalTraining.setMedicalSchool(school);

        em.persist(newMedicalTraining);
        return newMedicalTraining;
    }



    @Transactional
    public void deleteMedicalTrainingById(int id) {
        MedicalTraining newMedicalTraining = getById(MedicalTraining.class, MedicalTraining.FIND_BY_ID, id);
        if (newMedicalTraining != null) {
            em.remove(newMedicalTraining);
        }
    }

    public MedicalTraining getMedicalTrainingById(int id) {
        TypedQuery<MedicalTraining> query = em.createNamedQuery(MedicalTraining.FIND_BY_ID, MedicalTraining.class);
        query.setParameter(PARAM1, id);
        return query.getSingleResult();
    }
    @Transactional
    public MedicalTraining updateMedicalTrainingById(int id, MedicalTraining updatedMedicalTraining) {
        MedicalTraining trainingToBeUpdated = getMedicalTrainingById(id);
        if (trainingToBeUpdated != null) {
            em.refresh(trainingToBeUpdated); // Ensure entity is up-to-date

            // Update fields
            trainingToBeUpdated.setMedicalSchool(updatedMedicalTraining.getMedicalSchool());
            trainingToBeUpdated.getDurationAndStatus().setStartDate(updatedMedicalTraining.getDurationAndStatus().getStartDate());
            trainingToBeUpdated.getDurationAndStatus().setEndDate(updatedMedicalTraining.getDurationAndStatus().getEndDate());
            trainingToBeUpdated.getDurationAndStatus().setActive(updatedMedicalTraining.getDurationAndStatus().getActive());

            
            // Save the changes
            em.merge(trainingToBeUpdated);
        }
        return trainingToBeUpdated;
    }

    // ************************************
    // MEDICINE MANAGEMENT
    // ************************************

    public List<Medicine> getAllMedicines() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Medicine> cq = cb.createQuery(Medicine.class);
        cq.select(cq.from(Medicine.class));
        return em.createQuery(cq).getResultList();
    }

    public Medicine getMedicineById(int id) {
        return em.find(Medicine.class, id);
    }
//    public Medicine getMedicineById(int id) {
//        LOG.debug("Attempting to fetch medicine with ID: {}", id);
//        try {
//            Medicine medicine = em.find(Medicine.class, id);
//            LOG.debug("Medicine fetched: {}", medicine);
//            return medicine;
//        } catch (Exception e) {
//            LOG.error("Error fetching medicine with ID: {}", id, e);
//            return null;
//        }
//    }



    @Transactional
    public void persistMedicine(Medicine newMedicine) {
        em.persist(newMedicine);
    }

    @Transactional
    public void deleteMedicineById(int id) {
        Medicine medicine = getById(Medicine.class, Medicine.MEDICINE_BY_ID, id);
        if (medicine != null) {
            em.remove(medicine);
        }
    }

    // ************************************
    // PATIENT MANAGEMENT
    // ************************************

    public List<Patient> getAllPatients() {
        TypedQuery<Patient> query = em.createNamedQuery(Patient.ALL_PATIENTS_QUERY, Patient.class);
        return query.getResultList();
    }

    public Patient getPatientById(int id) {
        return em.find(Patient.class, id);
    }

    @Transactional
    public Patient persistPatient(Patient newPatient) {
        em.persist(newPatient);
        return newPatient;
    }

    @Transactional
    public void deletePatientById(int id) {
        Patient patient = em.find(Patient.class, id);
        if (patient != null) {
            em.remove(patient);
        }
    }
    
    @Transactional
    public Patient updatePatientById(int id, Patient updatedPatient) {
        Patient existingPatient = em.find(Patient.class, id);
        if (existingPatient != null) {
            em.refresh(existingPatient);

            // Update fields
            existingPatient.setFirstName(updatedPatient.getFirstName());
            existingPatient.setLastName(updatedPatient.getLastName());
            existingPatient.setYear(updatedPatient.getYear());
            existingPatient.setAddress(updatedPatient.getAddress());
            existingPatient.setHeight(updatedPatient.getHeight());
            existingPatient.setWeight(updatedPatient.getWeight());
            existingPatient.setSmoker(updatedPatient.getSmoker());

            em.merge(existingPatient);
            em.flush();
            return existingPatient;
        }
        return null;
    }
    // ************************************
    // Medical Certificate
    // ************************************
    
 // Code for MedicalCertificateResource class
    public List<MedicalCertificate> getMedicalCertificates() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MedicalCertificate> cq = cb.createQuery(MedicalCertificate.class);
        cq.select(cq.from(MedicalCertificate.class));
        return em.createQuery(cq).getResultList();
    }

    // Code for MedicalCertificateResource class
    // Only a ‘USER_ROLE’ user can read their own MedicalCertificate
    public MedicalCertificate getMedicalCertificateById(int id) {
        return em.find(MedicalCertificate.class, id);
    }

    // Code for MedicalCertificateResource class
    @Transactional
    public MedicalCertificate persistMedicalCertificateForPhysician(int physicianId, int trainingId) {
        LOG.debug("inside persistMedicalCertificateForPhysician...");
        
        // Fetch the Physician and MedicalTraining from the database
        Physician physicianToBeUpdated = em.find(Physician.class, physicianId);
        MedicalTraining medicaltraining = em.find(MedicalTraining.class, trainingId);
        
        // Check if both entities exist
        if (physicianToBeUpdated == null) {
            LOG.error("Physician with id {} not found.", physicianId);
            throw new EntityNotFoundException("Physician not found for id " + physicianId);
        }
        if (medicaltraining == null) {
            LOG.error("MedicalTraining with id {} not found.", trainingId);
            throw new EntityNotFoundException("MedicalTraining not found for id " + trainingId);
        }

        // Create a new MedicalCertificate
        MedicalCertificate mc = new MedicalCertificate(medicaltraining, physicianToBeUpdated, (byte) 0);
        
        // Persist the MedicalCertificate entity
        em.persist(mc);
        
        // Return the newly created MedicalCertificate
        return mc;
    }

    // Code for MedicalCertificateResource class
    @Transactional
    public MedicalCertificate deleteMedicalCertificateById(int mId) {
    	LOG.debug("inside deleteMedicalCertificateById...");
    	MedicalCertificate mc = getMedicalCertificateById(mId);
        if (mc != null) {
            em.refresh(mc);
//    		CriteriaBuilder builder = em.getCriteriaBuilder();
    		//Medical Certificate will be removed if training is deleted
            em.remove(mc);
            return mc;
        }
        return null;		
    }



    // ************************************
    // GENERIC METHODS
    // ************************************

    public <T> List<T> getAll(Class<T> entity, String namedQuery) {
        TypedQuery<T> allQuery = em.createNamedQuery(namedQuery, entity);
        return allQuery.getResultList();
    }

    public <T> T getById(Class<T> entity, String namedQuery, int id) {
        TypedQuery<T> allQuery = em.createNamedQuery(namedQuery, entity);
        allQuery.setParameter(PARAM1, id);
        return allQuery.getSingleResult();
    }
}
