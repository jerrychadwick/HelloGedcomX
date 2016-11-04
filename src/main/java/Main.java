/**
 * Created by jchadwic on 10/28/16.
 */

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.familysearch.platform.FamilySearchPlatform;
import org.familysearch.platform.ordinances.Ordinance;
import org.gedcomx.Gedcomx;
import org.gedcomx.common.ResourceReference;
import org.gedcomx.common.URI;
import org.gedcomx.conclusion.*;
import org.gedcomx.source.SourceDescription;
import org.gedcomx.source.SourceReference;
import org.gedcomx.types.RelationshipType;
import org.gedcomx.util.DocMap;

import java.util.List;


public class Main {
    static final int NUMGENS = 6;
    static int iAncestorCount = 0;

    public static final String GEDCOMX_XML_MEDIA_TYPE = "application/x-gedcomx-v1+xml";
    public static final String FS_PLATFORM_V1_XML_MEDIA_TYPE = "application/x-fs-v1+xml";
    public static final String FS_ARK_URL_BASE = "https://familysearch.org/ark:/61903/4:1:";

    private static Client client = Client.create(new DefaultClientConfig());

    public static void main(String[] args) {
        // Pass a valid FamilySearch session ID as a command line argument.
        String sessionId = args[0];

        getPersonAndParents(FS_ARK_URL_BASE + "KWCB-QWK", 1, sessionId);
        System.out.println("Total count = " + iAncestorCount);

    }


    private static FamilySearchPlatform readGedcomxTree(String url, String sessionId) {
//    private static Gedcomx readGedcomx(String url, String sessionId, String mediaType) {
        WebResource.Builder builder = client.resource(url).getRequestBuilder().accept(FS_PLATFORM_V1_XML_MEDIA_TYPE);
        if (sessionId != null) {
            builder = builder.header("Authorization", "Bearer " + sessionId);
            builder = builder.header("From", "Jerry Chadwick");
        }
        ClientResponse response = builder.get(ClientResponse.class);
        if (response.getStatus() == 200 && response.getType().toString().equals(FS_PLATFORM_V1_XML_MEDIA_TYPE)) {
            return response.getEntity(FamilySearchPlatform.class);
        }
        System.out.println("Error: got response code " + response.getStatus() + " for URL " + url);
        return null;
    }

    private static Gedcomx readGedcomxRecords(String url, String sessionId) {
        WebResource.Builder builder = client.resource(url).getRequestBuilder().accept(GEDCOMX_XML_MEDIA_TYPE);
        if (sessionId != null) {
            builder = builder.header("Authorization", "Bearer " + sessionId);
            builder = builder.header("From", "Jerry Chadwick");
        }
        try {
            ClientResponse response = builder.get(ClientResponse.class);
            if (response.getStatus() == 200 && response.getType().toString().equals(GEDCOMX_XML_MEDIA_TYPE)) {
                return response.getEntity(Gedcomx.class);
            }
            else
                System.out.println("Error: got response code " + response.getStatus() + " for URL " + url);
        }
        catch (ClientHandlerException e) {
            //do something clever with the exception
            System.out.println(e.getMessage());
        }

        return null;
    }

    private  static void getPersonAndParents(String treePersonURL, int iLevel, String sessionId) {
        iAncestorCount++;
        // Read person
        FamilySearchPlatform treePerson = (FamilySearchPlatform) readGedcomxTree(treePersonURL, sessionId);
        String parentLabel;
        switch (iLevel) {
            case 1: parentLabel = "Me"; break;
            case 2: parentLabel = "Parent"; break;
            case 3: parentLabel = "GrandParent"; break;
            case 4: parentLabel = "1st Great-Grandparent"; break;
            case 5: parentLabel = "2nd Great-Grandparent"; break;
            case 6: parentLabel = "3rd Great-Grandparent"; break;
            default: parentLabel = iLevel + "th Great-Grandparent";
        }
        String sTreePersonName = printPersonSummary(treePerson, "Family Tree person details -- Relation = " + parentLabel);
        System.out.println();
        System.out.println("Sources for " + sTreePersonName + ":");
        System.out.println();

        List<Person> personsArray = treePerson.getPersons();
        if (personsArray != null) {
            List<SourceReference> sourcesRefList = personsArray.get(0).getSources();
            if (sourcesRefList != null) {
                for (SourceReference sourceRef : sourcesRefList) {
                    URI sourceURI = sourceRef.getDescriptionRef();
                    System.out.println(sourceURI.toString());
                    Gedcomx recordPersona = readGedcomxRecords(sourceURI.toString(), sessionId);
                    List<SourceDescription> sourceDescList = recordPersona.getSourceDescriptions();
                    URI sourceDescURI = sourceDescList.get(0).getAbout();
                    if (sourceDescURI != null) {
                        Gedcomx recordData = readGedcomxRecords(sourceDescURI.toString(), sessionId);
                        if (recordData != null) {
                            if (recordData.getPersons() != null) {
                                for (Person persona : recordData.getPersons()) {
                                    if (persona != null) {
                                        if (persona.getName() != null) {
                                            String sFactLabel = "";
                                            String sFactDate = "";
                                            String sFactPlace = "";
                                            String sPersonaName = persona.getName().getNameForms().get(0).getFullText();
                                            System.out.println("========== Persona name: " + sPersonaName + "==========");
                                            String factType;
                                            Date factDate;
                                            PlaceReference factPlace;
                                            if (persona.getFacts() != null) {
                                                for (Fact fact : persona.getFacts()) {
                                                    if (fact != null) {
                                                        factDate = fact.getDate();
                                                        if (factDate != null)
                                                            sFactDate =  factDate.getOriginal();

                                                        factPlace = fact.getPlace();
                                                        if (factPlace != null)
                                                            sFactPlace = factPlace.getOriginal();

                                                        // System.out.println("Fact info:");
                                                        factType = fact.getType().toString().replaceAll(".*/", "");
                                                        if (factType != null) {
                                                            if (factType.equals("Census")) {
                                                                sFactLabel = sPersonaName + " was found in the " + sFactDate + " census at " + sFactPlace;
                                                            }
                                                            else if (factType.equals("Birth")) {
                                                                sFactLabel = sPersonaName + " was born in " + sFactDate + " at " + sFactPlace;
                                                            }
                                                            else if (factType.equals("Death")) {
                                                                sFactLabel = sPersonaName + " died in " + sFactDate + " at " + sFactPlace;
                                                            }
                                                            else if (factType.equals("MaritalStatus")) {
                                                                sFactLabel = sPersonaName + " was married in " + sFactDate + " at " + sFactPlace;
                                                            }
                                                            else if (factType.equals("Residence")) {
                                                                sFactLabel = sPersonaName + " in " + sFactDate + " resided at " + sFactPlace;
                                                            }
                                                            else {
                                                                sFactLabel = "Fact type: " + factType + "   Date: " + sFactDate + "   Place: " + sFactPlace;
                                                            }
                                                        }
                                                        System.out.println(sFactLabel);
                                                        System.out.println();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    System.out.println();
                }
            }
        }
        System.out.println("End of " + sTreePersonName + " data =============");
        System.out.println();

        if (treePerson.getRelationships() != null) {
            for (Relationship relationship : treePerson.getRelationships()) {
                if (relationship.getKnownType() == RelationshipType.ParentChild) {
                    // get ID of treePerson
                    String personID = personsArray.get(0).getId();
                    // compare the ID of the treePerson with Person2
                    if (relationship.getPerson2().getResourceId().equals(personID)) {
                        // treePerson is the child; i.e. this is a parent to treePerson
                        FamilySearchPlatform parentPerson = (FamilySearchPlatform) readGedcomxTree(relationship.getPerson1().getResource().toString(), sessionId);
                        if (iLevel < NUMGENS) {
                            getPersonAndParents(FS_ARK_URL_BASE + relationship.getPerson1().getResourceId().toString() + "?sourceDescriptions", iLevel+1, sessionId);
                        }
                    }
                }
            }
        }

    }

    private static String printPersonSummary(FamilySearchPlatform gedcomx, String title) {
        String retVal = "";
        System.out.println();
        System.out.println("=============================================================================================================");
        System.out.println(title + ":");
        System.out.println();

        if (gedcomx != null) {
            if (gedcomx.getPersons() != null) {
                for (Person person : gedcomx.getPersons()) {
                    System.out.println("Person " + person.getId() + (person.getPersistentId() == null ? "" : " (" + person.getPersistentId().toString() + ")"));
                    if (person.getNames() != null) {
                        for (Name name : person.getNames()) {
                            if (name.getNameForms() != null) {
                                for (NameForm nameForm : name.getNameForms()) {
                                    if (nameForm.getFullText() != null) {
                                        retVal = nameForm.getFullText();
                                        System.out.println("  Name: " + retVal);
                                    }
                                    if (nameForm.getParts() != null) {
                                        for (NamePart namePart : nameForm.getParts()) {
                                            System.out.println("    " + namePart.getKnownType().name() + ": " + namePart.getValue());
                                        }
                                    }
                                }
                            }
                            break; // only want one name for my printout. Comment this line out to get all names.
                        }
                    }
                    if (person.getFacts() != null) {
                        for (Fact fact : person.getFacts()) {
                            String sFactType = fact.getType().toString().replaceAll(".*/", "");
                            if (sFactType.contains("data:") || sFactType.contains("LifeSketch") || sFactType.contains("Residence"))
                                continue;
                            else {
                                System.out.println("  " + fact.getType().toString().replaceAll(".*/", "") + ": " + // strip off fact type URI until last slash
                                        (fact.getValue() != null ? fact.getValue() : ""));
                                if (fact.getDate() != null) {
                                    System.out.println("    Date: " + fact.getDate().getOriginal());
                                }
                                if (fact.getPlace() != null) {
                                    System.out.println("    Place: " + fact.getPlace().getOriginal());
                                }
                            }
                        }
                    }
                    // Display ordinances
                    if (person.getExtensionElements() != null) {
                        for (Object o : person.getExtensionElements()) {
                            if (o instanceof Ordinance) {
                                Ordinance ord = (Ordinance) o;
                                if (ord.getTempleCode() != null || ord.getDate() != null) {
                                    System.out.println("  " + ord.getKnownType().name() + ": " +
                                            (ord.getDate() == null || ord.getDate().getFormal() == null ? "" : ord.getDate().getFormal() + "; ") +
                                            (ord.getTempleCode() == null ? "" : ord.getTempleCode()));
                                }
                            }
                        }
                    }

                }
            }
        }

        System.out.println();
        return retVal;
    }


    private static void printPersonRelationships(FamilySearchPlatform gedcomx, String title) {
        System.out.println();
        System.out.println(title + " ===============================");

        if (gedcomx != null) {
            DocMap docMap = new DocMap(gedcomx);
            if (gedcomx.getRelationships() != null) {
                for (Relationship relationship : gedcomx.getRelationships()) {
                    System.out.println(relationship.getType().toString().replaceAll(".*/", "") + " relationship:");
                    System.out.println("  p1: " + relationship.getPerson1().getResource().toString() + getName(relationship.getPerson1(), docMap));
                    System.out.println("  p2: " + relationship.getPerson2().getResource().toString() + getName(relationship.getPerson2(), docMap));
                }
            }
        }
    }

    private static String getName(ResourceReference ref, DocMap docMap) {
        Person person = docMap.getPerson(ref);
        if (person != null && person.getName() != null && person.getName().getNameForm() != null && person.getName().getNameForm().getFullText() != null) {
            return " (" + person.getName().getNameForm().getFullText() + ")";
        }
        return "";
    }
}