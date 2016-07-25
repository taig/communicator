//import java.io.{File, PrintWriter}
//import java.text.SimpleDateFormat
//import java.time.format.DateTimeFormatter
//import java.util.Date
//import java.util.Formatter.DateTime
//
//import sbt.Keys._
//import sbt.{Project, SimpleReader, State}
//import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys._
//import sbtrelease.ReleasePlugin.autoImport._
//import sbtrelease.{ReleaseStateTransformations, Vcs}
//import sbtrelease.ReleaseStateTransformations._
//import sbtrelease.Utilities._
//import tut.Plugin._
//
//import scala.io.Source
//
//object Release {
//    /**
//     * Specify release version, not asking for a next version in case of a snapshot release
//     */
//    lazy val inquireVersions: ReleaseStep = { state: State =>
//        val extracted = Project.extract( state )
//
//        val useDefs = state.get( useDefaults ).getOrElse( false )
//        val currentV = extracted.get( version )
//
//        val releaseFunc = extracted.get( releaseVersion )
//        val suggestedReleaseV = releaseFunc( currentV )
//
//        //flatten the Option[Option[String]] as the get returns an Option, and the value inside is an Option
//        val releaseV = readVersion(
//            suggestedReleaseV,
//            "Release version [%s] : ",
//            useDefs,
//            state.get( commandLineReleaseVersion ).flatten
//        )
//
//        val nextV = if( releaseV endsWith "-SNAPSHOT" ) {
//            releaseV
//        } else {
//            val nextFunc = extracted.get( releaseNextVersion )
//            val suggestedNextV = nextFunc( releaseV )
//            //flatten the Option[Option[String]] as the get returns an Option, and the value inside is an Option
//            readVersion(
//                suggestedNextV,
//                "Next version [%s] : ",
//                useDefs,
//                state.get( commandLineNextVersion ).flatten
//            )
//        }
//
//        state.put( versions, ( releaseV, nextV ) )
//    }
//
//    lazy val commitReleaseVersion: ReleaseStep = { state: State =>
//        val extracted = Project.extract( state )
//        val releaseV = extracted.get( version )
//
//        if( releaseV endsWith "-SNAPSHOT" ) {
//            state.log.info( "Skipping 'commitReleaseVersion' for snapshot release" )
//            state
//        } else {
//            ReleaseStateTransformations.commitReleaseVersion( state )
//        }
//    }
//
//    lazy val tagRelease: ReleaseStep = { state: State =>
//        val extracted = Project.extract( state )
//        val releaseV = extracted.get( version )
//        
//        if( releaseV endsWith "-SNAPSHOT" ) {
//            state.log.info( "Skipping 'tagRelease' for snapshot release" )
//            state
//        } else {
//            ReleaseStateTransformations.tagRelease( state )
//        }
//    }
//
//    lazy val setNextVersion: ReleaseStep = { state: State =>
//        val ( current, next ) = state.get( versions ).get
//        
//        if( current == next ) {
//            state.log.info( "Skipping 'setNextVersion' because current and next version are equal" )
//            state
//        } else {
//            ReleaseStateTransformations.setNextVersion( state )
//        }
//    }
//
//    lazy val commitNextVersion: ReleaseStep = { state: State =>
//        val extracted = Project.extract( state )
//        val releaseV = extracted.get( version )
//
//        if( releaseV endsWith "-SNAPSHOT" ) {
//            state.log.info( "Skipping 'commitNextVersion' for snapshot release" )
//            state
//        } else {
//            ReleaseStateTransformations.commitNextVersion( state )
//        }
//    }
//
//    lazy val pushChanges: ReleaseStep = { state: State =>
//        val extracted = Project.extract( state )
//        val releaseV = extracted.get( version )
//
//        if( releaseV endsWith "-SNAPSHOT" ) {
//            state.log.info( "Skipping 'pushChanges' for snapshot release" )
//            state
//        } else {
//            ReleaseStateTransformations.pushChanges( state )
//        }
//    }
//
//    lazy val updateChangelog: ReleaseStep = { state: State =>
//        def confirm( changelog: File ): Array[String] = {
//            val currentChangelog = Source.fromFile( changelog ).getLines().toArray
//
//            SimpleReader.readLine( "Done? [yes]: " ) match {
//                case Some( "" | "y" | "yes" ) =>
//                    val updatedChangelog = Source.fromFile( changelog ).getLines().toArray
//
//                    if( !( currentChangelog sameElements updatedChangelog ) ) { 
//                        state.log.info( "Changelog:" )
//                        state.log.info( "" )
//                        updatedChangelog.foreach( state.log.info( _ ) )
//                        state.log.info( "\nEdit this file to apply changes:" )
//                        state.log.info( changelog.getAbsolutePath )
//
//                        confirm( changelog )
//                    } else {
//                        updatedChangelog
//                    }
//                case _ => confirm( changelog )
//            }
//        }
//
//        val extracted = Project.extract( state )
//        val releaseV = extracted.get( version )
//
////        if( releaseV endsWith "-SNAPSHOT" ) {
////            state.log.info( "Skipping 'updateChangelog' for snapshot release" )
////            state
////        } else {
//            val vcs = state.extract
//                .get( releaseVcs )
//                .getOrElse(
//                    sys.error( "Aborting release. Working directory is not a repository of a recognized VCS." )
//                )
//            
//            val lastTag = ( vcs.cmd( "describe", "--abbrev=0", "--tags" ) !! state.log ).trim
//            val lastCommits = vcs.cmd( "log", s"$lastTag..HEAD", "--abbrev-commit", "--pretty=%s" )
//                .!!( state.log )
//                .trim
//                .split( "\n" )
//                .map( " * " + _ )
//
//            val changelog = File.createTempFile( "changelog", ".md" )
//
//            val printer = new PrintWriter( changelog )
//            printer.write( lastCommits.mkString( "\n" ) )
//            printer.close()
//
//            state.log.info( "Created changelog (based on commit history):" )
//            state.log.info( "" )
//            lastCommits.foreach( state.log.info( _ ) )
//            state.log.info( "\nEdit this file to apply changes:" )
//            state.log.info( changelog.getAbsolutePath )
//
//            val changes = confirm( changelog )
//            
//            {
//                // Read changelog file
//                val destination = new File( "documentation/src/main/tut/CHANGELOG.md" )
//                val content = Source.fromFile( destination ).getLines()
//                val ( header, body ) = content.partition( _.startsWith( "[//]" ) )
//                val printer = new PrintWriter( destination )
//                printer.write( header.mkString( "\n" ) + "\n" )
//                printer.write( "[//]: <> (?.?.?)\n" )
//                printer.write( "## ?.?.?\n" )
//                printer.write( "\n" )
//                val date = new SimpleDateFormat( "yyyy-MM-dd" ).format( new Date() )
//                printer.write( s"_${date}_\n" )
//                printer.write( "\n" )
//                printer.write( changes.mkString( "\n" ) )
//                printer.write( "\n" )
//                printer.write( body.mkString( "\n" ) )
//                printer.close()
//            }
//
//            state
////        }
//    }
//}