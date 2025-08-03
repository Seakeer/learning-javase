package me.seakeer.learning.javase.iostream;

import java.io.*;

/**
 * ObjectSerialize;
 *
 * @author Seakeer;
 * @date 2024/9/28;
 */
public class ObjectIoStreamExample {

    public static void main(String[] args) {
        defaultSerializationExample();
        customSerializationExample();
    }

    private static void defaultSerializationExample() {
        serializeDeserialize(new Child("TRANSIENT_FIELD", "SAME_FIELD_CHILD", "CHILD_FIELD",
                new Parent("PARENT_FIELD", "SAME_FIELD_PARENT")));
    }

    private static <T> void serializeDeserialize(T obj) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream)) {
            oos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))) {
            T newObj = (T) ois.readObject();
            System.out.println(newObj);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void customSerializationExample() {
        serializeDeserialize(new CustomByExternalizable("key", 1, 2L));
        serializeDeserialize(new CustomByObjectStreamField("key", 1, 2L));
        serializeDeserialize(new CustomByMethod("key", 1, 2L));
    }


    public static class CustomByExternalizable implements Externalizable {

        private String key;

        private Integer value;

        private Long na;

        public CustomByExternalizable() {
        }


        public CustomByExternalizable(String key, Integer value, Long na) {
            this.key = key;
            this.value = value;
            this.na = na;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(key);
            out.writeObject(value);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            key = (String) in.readObject();
            value = (Integer) in.readObject();
        }

        @Override
        public String toString() {
            return "CustomByExternalizable{" +
                    "key='" + key + '\'' +
                    ", value=" + value +
                    ", na=" + na +
                    '}';
        }
    }

    public static class CustomByObjectStreamField implements Serializable {

        private String key;

        private Integer value;

        private Long na;

        public CustomByObjectStreamField(String key, Integer value, Long na) {
            this.key = key;
            this.value = value;
            this.na = na;
        }

        private static final ObjectStreamField[] serialPersistentFields = {
                new ObjectStreamField("key", String.class),
                new ObjectStreamField("value", Integer.class)
        };

        @Override
        public String toString() {
            return "CustomByObjectStreamField{" +
                    "key='" + key + '\'' +
                    ", value=" + value +
                    ", na=" + na +
                    '}';
        }
    }

    public static class CustomByMethod implements Serializable {

        private String key;

        private Integer value;

        private Long na;

        public CustomByMethod(String key, Integer value, Long na) {
            this.key = key;
            this.value = value;
            this.na = na;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(key);
            out.writeObject(value);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            key = (String) in.readObject();
            value = (Integer) in.readObject();
        }

        private void readObjectNoData() throws ObjectStreamException {

        }

        @Override
        public String toString() {
            return "CustomByMethod{" +
                    "key='" + key + '\'' +
                    ", value=" + value +
                    ", na=" + na +
                    '}';
        }
    }


    public static class SuperParent {

        private String superParentField;

        private String sameField;

        public SuperParent(String superParentField, String sameField) {
            this.superParentField = superParentField;
            this.sameField = sameField;
        }

        public SuperParent() {
        }

        @Override
        public String toString() {
            return "SuperParent{" +
                    "superParentField='" + superParentField + '\'' +
                    ", sameField='" + sameField + '\'' +
                    '}';
        }
    }

    public static class Parent extends SuperParent implements Serializable {

        private String parentField;

        private String sameField;

        public Parent(String parentField, String sameField) {
            super("SUPER_PARENT_FIELD", "SAME_FIELD_SUPER");
            this.parentField = parentField;
            this.sameField = sameField;
        }

        @Override
        public String toString() {
            return "Parent{" +
                    "parentField='" + parentField + '\'' +
                    ", sameField='" + sameField + '\'' +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    public static class Child extends Parent {

        private static final String staticFinalField = "STATIC_FINALE_FIELD";
        private static String staticField = "STATIC_FIELD";
        private final String finalField;


        private transient String transientField;

        private String sameField;

        private String childField;

        private Parent parent;

        public Child(String transientField, String sameField, String childField, Parent parent) {
            super("PARENT_FIELD", "SAME_FIELD_PARENT");
            this.transientField = transientField;
            this.sameField = sameField;
            this.childField = childField;
            this.parent = parent;
            this.finalField = "FINAL_FIELD";
        }

        @Override
        public String toString() {
            return "Child{" +
                    "staticFinalField='" + staticFinalField + '\'' +
                    ", staticField='" + staticField + '\'' +
                    ", finalField='" + finalField + '\'' +
                    ", transientField='" + transientField + '\'' +
                    ", sameField='" + sameField + '\'' +
                    ", childField='" + childField + '\'' +
                    ", parent=" + parent +
                    '}';
        }
    }

}
