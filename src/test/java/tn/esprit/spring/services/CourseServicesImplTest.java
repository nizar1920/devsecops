package tn.esprit.spring.services;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.esprit.spring.entities.Course;
import tn.esprit.spring.repositories.ICourseRepository;

public class CourseServicesImplTest {

    @Mock
    private ICourseRepository courseRepository;

    @InjectMocks
    private CourseServicesImpl courseServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRetrieveAllCourses() {
        Course course1 = new Course();
        Course course2 = new Course();
        List<Course> courses = Arrays.asList(course1, course2);

        when(courseRepository.findAll()).thenReturn(courses);

        List<Course> result = courseServices.retrieveAllCourses();

        verify(courseRepository).findAll(); // Vérifie que la méthode findAll a été appelée
        assert(result.size() == 2);
    }

    @Test
    void testAddCourse() {
        Course course = new Course();
        when(courseRepository.save(course)).thenReturn(course);

        Course result = courseServices.addCourse(course);

        verify(courseRepository).save(course);
        assert(result != null);
    }

    @Test
    void testUpdateCourse() {
        Course course = new Course();
        when(courseRepository.save(course)).thenReturn(course);

        Course result = courseServices.updateCourse(course);

        verify(courseRepository).save(course);
        assert(result != null);
    }

    @Test
    void testRetrieveCourse() {
        Long courseId = 1L;
        Course course = new Course();
        when(courseRepository.findById(courseId)).thenReturn(java.util.Optional.of(course));

        Course result = courseServices.retrieveCourse(courseId);

        verify(courseRepository).findById(courseId);
        assert(result != null);
    }
}
