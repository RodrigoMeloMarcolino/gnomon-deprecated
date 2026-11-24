import process from "node:process";
const files = JSON.parse(process.env.PR_FILES);
const integration = files.some((file) => /^(src\/(main|test)\/|pom\.xml|\.github\/workflows\/|config\/|src\/main\/resources\/)/.test(file));
console.log(`integration=${integration}`);
