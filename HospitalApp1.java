import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HospitalApp1 {

    static ArrayList<Patient> patients = new ArrayList<>();
    static ArrayList<Doctor> doctors = new ArrayList<>();
    static ArrayList<Appointment> appointments = new ArrayList<>();
    static ArrayList<Integer> bookedPatientIds = new ArrayList<>(); // boxing / unboxing
    static int patientCounter = 1;
    static int appointmentCounter = 1;

    // File saving
    static final Path PATIENT_FILE = Paths.get("patients.txt");
    static final Path APPOINTMENT_FILE = Paths.get("appointments.txt");

    // Date format (strict, so 31-02-2026 is rejected)
    static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);

    // Varargs
    static void showPatients(Patient... list) {
        if (list.length == 0) {
            System.out.println("No patients found.");
            return;
        }
        System.out.println("\n--------------------------------------------------------");
        System.out.printf("%-5s %-15s %-5s %-15s %-12s%n", "ID", "Name", "Age", "Disease", "Appointment");
        System.out.println("--------------------------------------------------------");
        for (Patient p : list) {
            int id = p.getId(); // auto-unboxing is used in contains() below
            String status = bookedPatientIds.contains(id) ? "Booked" : "Not booked";
            System.out.printf("%-5d %-15s %-5d %-15s %-12s%n",
                    p.getId(), p.getName(), p.getAge(), p.getDisease(), status);
        }
        System.out.println("--------------------------------------------------------");
    }

    static void showAppointments() {
        if (appointments.isEmpty()) {
            System.out.println("\nNo appointments booked yet.");
            return;
        }
        System.out.println("\n------------------------------------------------------------------------");
        System.out.printf("%-5s %-15s %-30s %-12s%n", "ID", "Patient", "Doctor", "Date");
        System.out.println("------------------------------------------------------------------------");
        for (Appointment a : appointments) {
            System.out.printf("%-5d %-15s %-30s %-12s%n",
                    a.getAppId(),
                    a.getPatient().getName(),
                    a.getDoctor().getName() + " (" + a.getDoctor().getSpecialization() + ")",
                    a.getDate());
        }
        System.out.println("------------------------------------------------------------------------");
    }

    static Patient findPatient(int id) throws PatientNotFoundException {
        for (Patient p : patients) {
            if (p.getId() == id) return p;
        }
        throw new PatientNotFoundException("Patient with ID " + id + " not found!");
    }

    static Doctor findDoctor(int id) {
        for (Doctor d : doctors) {
            if (d.getId() == id) return d;
        }
        return null;
    }

    static int readInt(Scanner sc, String message) {
        while (true) {
            System.out.print(message);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
            }
        }
    }

    // Returns null if the date is not valid
    static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim().replace('/', '-'), DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ---------- Save / Load ----------
    static void saveData() {
        try {
            List<String> pLines = new ArrayList<>();
            for (Patient p : patients) {
                pLines.add(p.getId() + "|" + p.getName() + "|" + p.getAge() + "|" + p.getDisease());
            }
            Files.write(PATIENT_FILE, pLines);

            List<String> aLines = new ArrayList<>();
            for (Appointment a : appointments) {
                aLines.add(a.getAppId() + "|" + a.getPatient().getId() + "|"
                        + a.getDoctor().getId() + "|" + a.getDate());
            }
            Files.write(APPOINTMENT_FILE, aLines);
        } catch (IOException e) {
            System.out.println("Warning: could not save data (" + e.getMessage() + ")");
        }
    }

    static void loadData() {
        try {
            if (Files.exists(PATIENT_FILE)) {
                for (String line : Files.readAllLines(PATIENT_FILE)) {
                    String[] f = line.split("\\|");
                    if (f.length == 4) {
                        int id = Integer.parseInt(f[0]);
                        patients.add(new Patient(id, f[1], Integer.parseInt(f[2]), f[3]));
                        if (id >= patientCounter) patientCounter = id + 1;
                    }
                }
            }
            if (Files.exists(APPOINTMENT_FILE)) {
                for (String line : Files.readAllLines(APPOINTMENT_FILE)) {
                    String[] f = line.split("\\|");
                    if (f.length == 4) {
                        int appId = Integer.parseInt(f[0]);
                        int pid = Integer.parseInt(f[1]);
                        Doctor doc = findDoctor(Integer.parseInt(f[2]));
                        try {
                            Patient pt = findPatient(pid);
                            if (doc != null) {
                                appointments.add(new Appointment(appId, pt, doc, f[3]));
                                bookedPatientIds.add(pid);
                                if (appId >= appointmentCounter) appointmentCounter = appId + 1;
                            }
                        } catch (PatientNotFoundException e) {
                            // skip appointments whose patient no longer exists
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Warning: could not load saved data (" + e.getMessage() + ")");
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Pre-loaded doctors
        doctors.add(new Doctor(1, "Dr. Sharma", 45, "General Physician"));
        doctors.add(new Doctor(2, "Dr. Meena", 38, "Pediatrician"));
        doctors.add(new Doctor(3, "Dr. Kumar", 50, "Cardiologist"));

        loadData();

        int choice;
        do {
            System.out.println("\n================================");
            System.out.println("  HOSPITAL MANAGEMENT SYSTEM");
            System.out.println("================================");
            System.out.println("1. Register Patient");
            System.out.println("2. View Patients");
            System.out.println("3. Search Patient");
            System.out.println("4. View Doctors");
            System.out.println("5. Book Appointment");
            System.out.println("6. View Appointments");
            System.out.println("7. Cancel Appointment");
            System.out.println("8. Exit");
            choice = readInt(sc, "\nEnter your choice: ");

            switch (choice) {
                case 1:
                    System.out.print("\nEnter patient name: ");
                    String name = sc.nextLine().trim().replace("|", " ");
                    int age = readInt(sc, "Enter patient age: ");
                    System.out.print("Enter disease: ");
                    String disease = sc.nextLine().trim().replace("|", " ");
                    if (name.isEmpty() || age <= 0 || disease.isEmpty()) {
                        System.out.println("\nInvalid details! Registration failed.");
                        break;
                    }
                    Patient p = new Patient(patientCounter++, name, age, disease);
                    patients.add(p);
                    saveData();
                    System.out.println("\nPatient registered successfully!");
                    System.out.println("Patient ID: " + p.getId());
                    break;

                case 2:
                    showPatients(patients.toArray(new Patient[0]));
                    break;

                case 3:
                    int sid = readInt(sc, "\nEnter patient ID to search: ");
                    try {
                        Patient found = findPatient(sid);
                        showPatients(found);
                    } catch (PatientNotFoundException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    break;

                case 4:
                    System.out.println("\n----------------------------------------------");
                    System.out.printf("%-5s %-15s %-5s %-20s%n", "ID", "Name", "Age", "Specialization");
                    System.out.println("----------------------------------------------");
                    for (Doctor d : doctors) {
                        d.display();
                    }
                    System.out.println("----------------------------------------------");
                    break;

                case 5:
                    int pid = readInt(sc, "\nEnter patient ID: ");
                    try {
                        Patient pt = findPatient(pid);
                        if (bookedPatientIds.contains(pid)) {
                            System.out.println("Appointment already booked for this patient.");
                            break;
                        }
                        for (Doctor d : doctors) {
                            System.out.println(d.getId() + ". " + d.getName() + " (" + d.getSpecialization() + ")");
                        }
                        int did = readInt(sc, "Choose doctor ID: ");
                        Doctor doc = findDoctor(did);
                        if (doc == null) {
                            System.out.println("Invalid doctor ID!");
                            break;
                        }

                        System.out.print("Enter appointment date (dd-mm-yyyy): ");
                        LocalDate parsed = parseDate(sc.nextLine());
                        if (parsed == null) {
                            System.out.println("Invalid date! Use the format dd-mm-yyyy, e.g. 27-10-2026.");
                            break;
                        }
                        if (parsed.isBefore(LocalDate.now())) {
                            System.out.println("You cannot book an appointment in the past.");
                            break;
                        }
                        String date = parsed.format(DATE_FMT);

                        // Double-booking check: same doctor, same date
                        boolean clash = false;
                        for (Appointment existing : appointments) {
                            if (existing.getDoctor().getId() == did && existing.getDate().equals(date)) {
                                clash = true;
                                break;
                            }
                        }
                        if (clash) {
                            System.out.println(doc.getName() + " is already booked on " + date
                                    + ". Please choose another date or doctor.");
                            break;
                        }

                        Appointment ap = new Appointment(appointmentCounter++, pt, doc, date);
                        appointments.add(ap);
                        bookedPatientIds.add(pid); // autoboxing: int -> Integer
                        saveData();
                        System.out.println("\nAppointment booked successfully!");
                        ap.display();
                    } catch (PatientNotFoundException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    break;

                case 6:
                    showAppointments();
                    break;

                case 7:
                    if (appointments.isEmpty()) {
                        System.out.println("\nNo appointments to cancel.");
                        break;
                    }
                    showAppointments();
                    int cid = readInt(sc, "\nEnter appointment ID to cancel: ");
                    boolean cancelled = false;
                    for (int i = 0; i < appointments.size(); i++) {
                        if (appointments.get(i).getAppId() == cid) {
                            Appointment removed = appointments.remove(i);
                            // remove(Integer) removes the value, not the index
                            bookedPatientIds.remove(Integer.valueOf(removed.getPatient().getId()));
                            saveData();
                            System.out.println("Appointment " + cid + " cancelled.");
                            cancelled = true;
                            break;
                        }
                    }
                    if (!cancelled) {
                        System.out.println("Appointment not found.");
                    }
                    break;

                case 8:
                    System.out.println("\nThank you for using Hospital Management System!");
                    break;

                default:
                    System.out.println("Invalid choice! Try again.");
            }
        } while (choice != 8);

        sc.close();
    }
}

// Interface
interface Displayable {
    void display();
}

// Custom exception
class PatientNotFoundException extends Exception {
    public PatientNotFoundException(String message) {
        super(message);
    }
}

// Base class (inheritance)
abstract class Person {
    private int id;
    private String name;
    private int age;

    Person(int id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
}

class Patient extends Person {
    private String disease;

    Patient(int id, String name, int age, String disease) {
        super(id, name, age);
        this.disease = disease;
    }

    public String getDisease() { return disease; }
}

class Doctor extends Person implements Displayable {
    private String specialization;

    Doctor(int id, String name, int age, String specialization) {
        super(id, name, age);
        this.specialization = specialization;
    }

    public String getSpecialization() { return specialization; }

    public void display() {
        System.out.printf("%-5d %-15s %-5d %-20s%n", getId(), getName(), getAge(), specialization);
    }
}

class Appointment implements Displayable {
    private int appId;
    private Patient patient;
    private Doctor doctor;
    private String date;

    Appointment(int appId, Patient patient, Doctor doctor, String date) {
        this.appId = appId;
        this.patient = patient;
        this.doctor = doctor;
        this.date = date;
    }

    public int getAppId() { return appId; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public String getDate() { return date; }

    public void display() {
        System.out.println("Appointment ID: " + appId);
        System.out.println("Patient: " + patient.getName());
        System.out.println("Doctor: " + doctor.getName() + " (" + doctor.getSpecialization() + ")");
        System.out.println("Date: " + date);
    }
}