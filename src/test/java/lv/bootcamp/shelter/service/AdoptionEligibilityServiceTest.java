package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.audit.AuditLogger;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.model.Adopter;
import lv.bootcamp.shelter.repository.AdopterRepository;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.model.AdoptionResult;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static lv.bootcamp.shelter.service.RejectionReasons.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Write tests for AdoptionEligibilityService.
 * The class and mocks are set up — the rest is yours.
 */
@ExtendWith(MockitoExtension.class)
class AdoptionEligibilityServiceTest {

    private static final Long ADOPTER_ID = 1L;
    private static final Long ANIMAL_ID = 2L;

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AdoptionEligibilityService service;

    private Adopter validAdopter() {
        Adopter a = new Adopter();
        a.setId(ADOPTER_ID);
        a.setName("William");
        a.setEmail("william.atkinson@mail.com");
        a.setAge(45);
        a.setCurrentPetCount(0);
        a.setPreviousAdoptions(0);
        a.setLargeProperty(false);
        a.setExoticPermit(false);
        return a;
    }

    private Animal availableAnimal() {
        Animal an = new Animal();
        an.setId(ANIMAL_ID);
        an.setName("Lola");
        an.setType(AnimalType.DOG);
        an.setAge(2);
        an.setStatus(AnimalStatus.AVAILABLE);
        return an;
    }

    @Nested
    class RejectionReasonTests {

        @Test
        void rejectsWhenAdopterNotFound() {
            AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

            assertThat(result.approved()).isFalse();
            assertThat(result.reason()).isEqualTo(ADOPTER_NOT_FOUND);
            assertThat(result.priorityScore()).isZero();
        }

        @Test
        void rejectsWhenAnimalNotFound() {
            when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.of(validAdopter()));
            when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.empty());

            AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

            assertThat(result.reason()).isEqualTo(ANIMAL_NOT_FOUND);
        }

        @Test
        void rejectsWhenAnimalNotAvailable() {
            Animal reserved = availableAnimal();
            reserved.setStatus(AnimalStatus.RESERVED);

            when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.of(validAdopter()));
            when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(reserved));

            AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

            assertThat(result.reason()).isEqualTo(ANIMAL_NOT_AVAILABLE);
        }
    }

    @Nested
    class AcceptanceReasonTests {
        @Test
        void approvesValidAdopter() {
            when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.of(validAdopter()));
            when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(availableAnimal()));

            AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

            assertThat(result.approved()).isTrue();
            assertThat(result.reason()).isEqualTo("Approved");
            assertThat(result.priorityScore()).isZero();
        }

        @Test
        void approvesExoticAnimalWhenAdopterHasPermit() {
            Adopter adopter = validAdopter();
            adopter.setExoticPermit(true);
            Animal bird = availableAnimal();
            bird.setType(AnimalType.BIRD);

            when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.of(adopter));
            when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(bird));

            AdoptionResult result = service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

            assertThat(result.approved()).isTrue();
        }

        @Test
        void approvesAdopterAtMinimumAgeBoundary() {
            Adopter adopter = validAdopter();
            adopter.setAge(18);
            when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.of(adopter));
            when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(availableAnimal()));

            assertThat(service.evaluateAdoption(ADOPTER_ID, ANIMAL_ID).approved()).isTrue();
        }

    }
}
