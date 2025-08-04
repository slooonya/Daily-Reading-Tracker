<a id="readme-top"></a>

<!-- PROJECT LOGO -->
<div align="center">
<img width="1680" height="200" alt="Banner" src="https://github.com/user-attachments/assets/ee10c4fa-c057-4628-9433-e191a8c35a4d" />
</div>    
<br>
  <details>
    <summary>Table of Contents</summary>
    <ol>
      <li>
        <a href="#about-the-project">About The Project</a>
        <ul>
          <li><a href="#built-with">Built With</a></li>
        </ul>
      </li>
      <li>
        <a href="#getting-started">Getting Started</a>
        <ul>
          <li><a href="#prerequisites">Prerequisites</a></li>
          <li><a href="#installation">Installation</a></li>
        </ul>
      </li>
      <li><a href="#contributing">Contributing</a></li>
      <li><a href="#contact">Contact</a></li>
    </ol>
  </details>


<!-- ABOUT THE PROJECT -->
## About The Project
This web application helps users track and manage their daily reading activities to make a lasting habit of reading. The application features user authentication, role-based access control, and administrative oversight to ensure a smooth and secure user experience.

<img width="1409" height="1899" alt="screenshots" src="https://github.com/user-attachments/assets/8cd66483-8923-4b5e-964c-e9c165a2e13e" />

### Built With

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white) ![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white) ![HTML5](https://img.shields.io/badge/html5-%23E34F26.svg?style=for-the-badge&logo=html5&logoColor=white) ![CSS3](https://img.shields.io/badge/css3-%231572B6.svg?style=for-the-badge&logo=css3&logoColor=white) ![JavaScript](https://img.shields.io/badge/javascript-%23323330.svg?style=for-the-badge&logo=javascript&logoColor=%23F7DF1E) ![MySQL](https://img.shields.io/badge/mysql-4479A1.svg?style=for-the-badge&logo=mysql&logoColor=white)

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- GETTING STARTED -->
## Getting Started
To get a local copy up and running follow these simple example steps.

### Prerequisites
Make sure you have the following installed on your system:

1. Java Development Kit (JDK) (version 11 or higher)
   - [Download JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
2. Maven (for dependency management and building the project)
   - [Download Maven](https://maven.apache.org/download.cgi)
3. Git (for cloning the repository)
   - [Download Git](https://git-scm.com/)
4. An IDE (e.g., IntelliJ IDEA, Eclipse, or VS Code) for development.

Verify installations:
```bash
java -version
mvn -version
git --version
```

### Installation

1. Clone the repository:

```bash
git clone https://github.com/slooonya/daily-reading-tracker.git
```

2. Navigate to the project directory:

 ```bash
 cd daily-reading-tracker
 ```

3. Configure the environment variables for the database:
   - Open src/main/resources/application.properties file <br>
   - Update the following parameters to match your database configuration:

```bash
spring.datasource.url=jdbc:mysql://<DB_HOST>:<DB_PORT>/<DB_NAME>
spring.datasource.username=<DB_USERNAME>
spring.datasource.password=<DB_PASSWORD>
 ```
   
4. Build the project using Maven:
```bash
mvn clean install
```
5. Open your browser and navigate to:
http://localhost:8080

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- CONTRIBUTING -->
## Contributing

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- CONTACT -->
## Contact

Sonya's email address: snmmnva@gmail.com

Project Link: [https://github.com/slooonya/Daily-Reading-Tracker](https://github.com/slooonya/Daily-Reading-Tracker)

<img width="1680" height="200" alt="Footer" src="https://github.com/user-attachments/assets/60ebd63d-7993-4d93-945a-7e168158f68b" />

<p align="right">(<a href="#readme-top">back to top</a>)</p>

