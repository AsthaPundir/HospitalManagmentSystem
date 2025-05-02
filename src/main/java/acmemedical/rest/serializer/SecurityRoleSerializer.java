package acmemedical.rest.serializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import acmemedical.entity.SecurityRole;

public class SecurityRoleSerializer extends StdSerializer<Set<SecurityRole>> implements Serializable {

    private static final long serialVersionUID = 1L;

    // Default constructor
    public SecurityRoleSerializer() {
        this(null);
    }

    // Constructor with type parameter
    public SecurityRoleSerializer(Class<Set<SecurityRole>> t) {
        super(t);
    }

    /**
     * Custom serialization to prevent infinite recursion caused by bidirectional relationships.
     * This method creates "hollow" copies of SecurityRole entities with only essential fields.
     */
    @Override
    public void serialize(Set<SecurityRole> originalRoles, JsonGenerator generator, SerializerProvider provider)
        throws IOException {
        
        // Create a new set for hollow roles
        Set<SecurityRole> hollowRoles = new HashSet<>();
        
        for (SecurityRole originalRole : originalRoles) {
            // Create a new instance of SecurityRole with essential fields only
            SecurityRole hollowRole = new SecurityRole();
            hollowRole.setId(originalRole.getId());
            hollowRole.setRoleName(originalRole.getRoleName());
            hollowRole.setUsers(null); // Prevent recursion by setting users to null
            hollowRoles.add(hollowRole);
        }
        
        // Write the hollow roles to the JSON output
        generator.writeObject(hollowRoles);
    }
}
