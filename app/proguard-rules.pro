# Los enums que se guardan y se releen por nombre. Sin esto R8 renombra las
# constantes, `Vista.valueOf("DIA")` lanza IllegalArgumentException al restaurar
# el estado, y el tema elegido vuelve en silencio a SISTEMA.
-keepclassmembers enum com.marcm.cronicasapetito.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room genera código que referencia las entidades por su forma; las consumer
# rules de la librería ya lo cubren, pero las entidades y el DAO se quedan
# legibles para que un stack trace de producción se pueda leer.
-keep class com.marcm.cronicasapetito.data.** { *; }
