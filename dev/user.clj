(ns user

  "Require `./dev.clj` if available on classpath.
  
   Each project can have its own private dev file, possibly copied from a `./templ.clj` file.

   Those files should be located under `./src/clj/dev` at the root of a project.")


;;;;;;;;;;


(try
  (require 'dev)
  (catch Throwable _ex))
